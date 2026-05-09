package com.gustavo.brilhante.cutestickers.stickers.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageProcessor @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "Sticker - ImageProcessor"
        const val STICKER_SIZE = 512
        const val TRAY_SIZE = 96
        private const val STICKER_MAX_BYTES = 100 * 1024   // WhatsApp limit: 100 KB
        private const val ANIMATED_STICKER_MAX_BYTES = 500 * 1024 // WhatsApp limit: 500 KB
        private const val TRAY_MAX_BYTES = 50 * 1024       // WhatsApp limit: 50 KB
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
        val bitmap = downloadBitmap(imageUrl)
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
        val request = Request.Builder().url(imageUrl).build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to download: ${response.code}")
            response.body?.bytes() ?: error("Empty body")
        }
        
        Log.d(TAG, "Original GIF size: ${bytes.size / 1024} KB")

        val tempInput = File.createTempFile("raw_anim", ".bin").apply { writeBytes(bytes) }
        
        var finalResult: File? = null

        for (config in compressionPipeline) {
            Log.d(TAG, "Compression attempt ${config.attempt}: scale=${config.scale}, quality=${config.quality}, fps=${config.targetFps ?: "orig"}")
            
            val encoded = encodeAnimatedWebP(tempInput, config)
            
            Log.d(TAG, "Encoded sticker size: ${encoded.length() / 1024} KB")
            
            if (encoded.length() <= ANIMATED_STICKER_MAX_BYTES) {
                encoded.copyTo(outputFile, overwrite = true)
                finalResult = outputFile
                Log.d(TAG, "Success at attempt ${config.attempt}. Final dimensions: ${config.scale}x${config.scale}, Final fps: ${config.targetFps ?: "orig"}")
                encoded.delete()
                break
            }
            encoded.delete()
        }

        tempInput.delete()
        finalResult ?: error("Failed to compress animated sticker below 500KB after all attempts")
    }

    private fun encodeAnimatedWebP(inputFile: File, config: CompressionConfig): File {
        val tempOutput = File.createTempFile("encoded_anim", ".webp")
        
        // Decoding frames using ImageDecoder (supported in API 28+)
        val frames = decodeFrames(inputFile)
        
        // Processing pipeline (FPS reduction, redundancy removal)
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
            Log.e(TAG, "Error encoding animated WebP", e)
            // Fallback to static WebP if animation encoding fails, 
            // but ensure it's a valid WebP file.
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
            
            if (frames.isEmpty()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    frames.add(Frame(bitmap, 100))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding frames", e)
        }
        
        if (frames.isEmpty()) {
            val dummyBitmap = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
            frames.add(Frame(dummyBitmap, 100))
        }
        return frames
    }

    private fun processFrames(frames: List<Frame>, config: CompressionConfig): List<Frame> {
        var result = frames
        
        // 1. Redução de FPS
        config.targetFps?.let { target ->
            val currentFps = 1000 / (frames.firstOrNull()?.durationMs ?: 100)
            val step = maxOf(1, currentFps / target)
            if (step > 1) {
                result = result.filterIndexed { index, _ -> index % step == 0 }
            }
        }
        
        // 2. Remoção de frames redundantes
        if (config.skipRedundant) {
            result = result.fold(mutableListOf<Frame>()) { acc, frame ->
                if (acc.isEmpty() || !isRedundant(acc.last().bitmap, frame.bitmap)) {
                    acc.add(frame)
                }
                acc
            }
        }
        
        return result
    }

    private fun isRedundant(b1: Bitmap, b2: Bitmap): Boolean {
        // Simple pixel-level check or generationId check
        return b1.generationId == b2.generationId
    }

    private fun saveAsPng(bitmap: Bitmap, outputFile: File, maxBytes: Int): File {
        FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return if (outputFile.length() > maxBytes) {
            saveAsWebP(bitmap, outputFile, maxBytes)
        } else {
            bitmap.recycle()
            outputFile
        }
    }

    private fun downloadBitmap(url: String): Bitmap {
        val request = Request.Builder().url(url).build()
        val bytes = okHttpClient.newCall(request).execute().use { response ->
            response.body?.bytes() ?: error("Empty response body for: $url")
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode image from: $url")
    }

    private fun resizeWithPadding(source: Bitmap, targetSize: Int): Bitmap {
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scale = minOf(
            targetSize.toFloat() / source.width,
            targetSize.toFloat() / source.height
        )
        val scaledW = (source.width * scale).toInt()
        val scaledH = (source.height * scale).toInt()
        val offsetX = (targetSize - scaledW) / 2f
        val offsetY = (targetSize - scaledH) / 2f
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        canvas.drawBitmap(scaled, offsetX, offsetY, Paint(Paint.ANTI_ALIAS_FLAG))
        scaled.recycle()
        // Note: we don't recycle 'source' here as it might be used for other attempts
        return result
    }

    private fun saveAsWebP(bitmap: Bitmap, outputFile: File, maxBytes: Int): File {
        val format = lossyFormat()
        for (quality in QUALITY_STEPS) {
            FileOutputStream(outputFile).use { bitmap.compress(format, quality, it) }
            if (outputFile.length() <= maxBytes) break
        }
        if (outputFile.length() > maxBytes) {
            error("Cannot compress sticker below ${maxBytes / 1024} KB (file: ${outputFile.length() / 1024} KB)")
        }
        bitmap.recycle()
        return outputFile
    }

    @Suppress("DEPRECATION")
    private fun lossyFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Bitmap.CompressFormat.WEBP_LOSSY
        else
            Bitmap.CompressFormat.WEBP
}
