package com.gustavo.brilhante.cutestickers.stickers.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import com.gustavo.brilhante.cutestickers.common.Logger
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageProcessor @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val downloader: StickerDownloader,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "Sticker - ImageProcessor"
        const val STICKER_SIZE = 512
        const val TRAY_SIZE = 96
        private const val STICKER_MAX_BYTES = 100 * 1024
        private const val ANIMATED_STICKER_MAX_BYTES = 500 * 1024
        private const val TRAY_MAX_BYTES = 50 * 1024
        private val QUALITY_STEPS = intArrayOf(80, 60, 40, 20)
    }

    private data class CompressionConfig(
        val attempt: Int,
        val scale: Int,
        val quality: Int,
        val targetFps: Int?,
        val skipRedundant: Boolean = false
    )

    private data class Frame(val bitmap: Bitmap, val durationMs: Int)

    private val compressionPipeline = listOf(
        CompressionConfig(1, 512, 90, null),
        CompressionConfig(2, 512, 80, 12),
        CompressionConfig(3, 512, 70, 10, skipRedundant = true),
        CompressionConfig(4, 384, 60, 8, skipRedundant = true)
    )

    fun downloadAndProcess(
        imageUrl: String,
        outputFile: File,
        size: Int = STICKER_SIZE
    ): Result<File> = runCatching {
        val bytes = runBlocking { downloader.download(imageUrl).getOrThrow() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode image from bytes")
        
        val resized = resizeWithPadding(bitmap, size)
        val maxBytes = if (size <= TRAY_SIZE) TRAY_MAX_BYTES else STICKER_MAX_BYTES
        
        if (outputFile.extension.lowercase() == "png") {
            saveAsPng(resized, outputFile, maxBytes)
        } else {
            saveAsWebP(resized, outputFile, maxBytes)
        }
        outputFile
    }

    fun downloadAndProcessAnimated(
        imageUrl: String,
        outputFile: File
    ): Result<File> = runCatching {
        val bytes = runBlocking { downloader.download(imageUrl).getOrThrow() }
        
        logger.d(TAG, "Original GIF size: ${bytes.size / 1024} KB")

        val tempInput = File.createTempFile("raw_anim", ".bin").apply { writeBytes(bytes) }
        
        var finalResult: File? = null

        for (config in compressionPipeline) {
            logger.d(TAG, "Compression attempt ${config.attempt}: scale=${config.scale}, quality=${config.quality}")
            
            val encoded = encodeAnimatedWebP(tempInput, config)
            
            logger.d(TAG, "Encoded sticker size: ${encoded.length() / 1024} KB")
            
            if (encoded.length() <= ANIMATED_STICKER_MAX_BYTES) {
                encoded.copyTo(outputFile, overwrite = true)
                finalResult = outputFile
                encoded.delete()
                break
            }
            encoded.delete()
        }

        tempInput.delete()
        finalResult ?: error("Failed to compress animated sticker below 500KB")
    }

    private fun encodeAnimatedWebP(inputFile: File, config: CompressionConfig): File {
        val tempOutput = File.createTempFile("encoded_anim", ".webp")
        val frames = decodeFrames(inputFile)
        val processedFrames = processFrames(frames, config)
        
        try {
            val encoder = com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder(
                context,
                config.scale, 
                config.scale
            )
            
            val webPConfig = com.aureusapps.android.webpandroid.encoder.WebPConfig(
                quality = config.quality.toFloat()
            )
            encoder.configure(webPConfig)
            
            var currentTimeMs = 0L
            processedFrames.forEach { frame ->
                val resized = resizeWithPadding(frame.bitmap, config.scale)
                encoder.addFrame(currentTimeMs, resized)
                currentTimeMs += frame.durationMs.toLong()
                resized.recycle()
            }
            
            val outputUri = android.net.Uri.fromFile(tempOutput)
            encoder.assemble(currentTimeMs, outputUri)
            encoder.release()
        } catch (e: Exception) {
            logger.e(TAG, "Error encoding animated WebP", e)
            val bitmap = frames.firstOrNull()?.bitmap ?: Bitmap.createBitmap(config.scale, config.scale, Bitmap.Config.ARGB_8888)
            saveAsWebP(bitmap, tempOutput, ANIMATED_STICKER_MAX_BYTES)
        } finally {
            processedFrames.forEach { it.bitmap.recycle() }
        }
        
        return tempOutput
    }

    private fun decodeFrames(file: File): List<Frame> {
        val frames = mutableListOf<Frame>()
        try {
            val bytes = file.readBytes()
            @Suppress("DEPRECATION")
            val movie = android.graphics.Movie.decodeByteArray(bytes, 0, bytes.size)
            
            if (movie != null && movie.duration() > 0) {
                val duration = movie.duration()
                val frameCount = minOf(15, maxOf(5, duration / 100)) 
                val interval = duration / frameCount
                
                for (i in 0 until frameCount) {
                    val bitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    movie.setTime(i * interval)
                    movie.draw(canvas, 0f, 0f)
                    frames.add(Frame(bitmap, interval))
                }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error decoding frames", e)
        }
        
        if (frames.isEmpty()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) frames.add(Frame(bitmap, 100))
        }
        return frames
    }

    private fun processFrames(frames: List<Frame>, config: CompressionConfig): List<Frame> {
        var result = frames
        config.targetFps?.let { target ->
            val currentFps = 1000 / (frames.firstOrNull()?.durationMs ?: 100)
            val step = maxOf(1, currentFps / target)
            if (step > 1) result = result.filterIndexed { index, _ -> index % step == 0 }
        }
        if (config.skipRedundant) {
            result = result.fold(mutableListOf<Frame>()) { acc, frame ->
                if (acc.isEmpty() || acc.last().bitmap.generationId != frame.bitmap.generationId) {
                    acc.add(frame)
                }
                acc
            }
        }
        return result
    }

    private fun saveAsPng(bitmap: Bitmap, outputFile: File, maxBytes: Int): File {
        FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return if (outputFile.length() > maxBytes) saveAsWebP(bitmap, outputFile, maxBytes)
        else { bitmap.recycle(); outputFile }
    }

    private fun resizeWithPadding(source: Bitmap, targetSize: Int): Bitmap {
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scale = minOf(targetSize.toFloat() / source.width, targetSize.toFloat() / source.height)
        val scaledW = (source.width * scale).toInt()
        val scaledH = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        canvas.drawBitmap(scaled, (targetSize - scaledW) / 2f, (targetSize - scaledH) / 2f, Paint(Paint.ANTI_ALIAS_FLAG))
        scaled.recycle()
        return result
    }

    private fun saveAsWebP(bitmap: Bitmap, outputFile: File, maxBytes: Int): File {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        for (quality in QUALITY_STEPS) {
            FileOutputStream(outputFile).use { bitmap.compress(format, quality, it) }
            if (outputFile.length() <= maxBytes) break
        }
        bitmap.recycle()
        return outputFile
    }
}
