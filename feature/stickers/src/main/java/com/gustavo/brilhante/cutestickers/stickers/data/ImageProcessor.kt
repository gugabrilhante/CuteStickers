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
internal open class ImageProcessor @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val downloader: StickerDownloader,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "Sticker - ImageProcessor"
        const val STICKER_SIZE = 512
        const val TRAY_SIZE = 96
        private const val STICKER_MAX_BYTES = 100 * 1024
        private const val ANIMATED_STICKER_MAX_BYTES = 480 * 1024
        private const val TRAY_MAX_BYTES = 50 * 1024
        private val QUALITY_STEPS = intArrayOf(80, 60, 40, 20)
    }

    private data class CompressionConfig(
        val attempt: Int,
        val scale: Int,
        val quality: Int,
        val dropFramesRatio: Int = 1
    )

    internal data class Frame(val bitmap: Bitmap, val durationMs: Int)

    private val compressionPipeline = listOf(
        CompressionConfig(1, 512, 90),
        CompressionConfig(2, 512, 80),
        CompressionConfig(3, 512, 70),
        CompressionConfig(4, 512, 60),
        CompressionConfig(5, 512, 50),
        CompressionConfig(6, 512, 40),
        CompressionConfig(7, 512, 30),
        CompressionConfig(8, 512, 30, dropFramesRatio = 2)
    )

    internal open fun createEncoder(width: Int, height: Int) = 
        com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder(context, width, height)

    fun downloadAndProcess(
        imageUrl: String,
        outputFile: File,
        size: Int = STICKER_SIZE,
        isCropped: Boolean = true
    ): Result<File> = runCatching {
        val bytes = runBlocking { downloader.download(imageUrl).getOrThrow() }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: error("Failed to decode image from bytes")

        logger.d(TAG, "Original Image: ${bitmap.width}x${bitmap.height}")
        
        val processed = if (isCropped) centerCrop(bitmap, size) else resizeWithPadding(bitmap, size)
        val maxBytes = if (size <= TRAY_SIZE) TRAY_MAX_BYTES else STICKER_MAX_BYTES
        
        if (outputFile.extension.lowercase() == "png") {
            saveAsPng(processed, outputFile)
        } else {
            saveAsWebP(processed, outputFile, maxBytes)
        }
        outputFile
    }

    fun downloadAndProcessAnimated(
        imageUrl: String,
        outputFile: File,
        isCropped: Boolean = true
    ): Result<File> = runCatching {
        val bytes = runBlocking { downloader.download(imageUrl).getOrThrow() }
        
        logger.d(TAG, "Original GIF size: ${bytes.size / 1024} KB")

        val tempInput = File.createTempFile("raw_anim", ".bin").apply { writeBytes(bytes) }
        
        var finalResult: File? = null

        for (config in compressionPipeline) {
            logger.d(TAG, "Compression attempt ${config.attempt}: scale=${config.scale}, quality=${config.quality}")
            
            val encoded = encodeAnimatedWebP(tempInput, config, isCropped)
            
            logger.d(TAG, "Encoded sticker size: ${encoded.length() / 1024} KB")
            
            if (encoded.length() <= ANIMATED_STICKER_MAX_BYTES) {
                logger.d(TAG, "Final sticker accepted: ${encoded.length() / 1024} KB")
                encoded.copyTo(outputFile, overwrite = true)
                finalResult = outputFile
                encoded.delete()
                break
            }
            encoded.delete()
        }

        tempInput.delete()
        finalResult ?: error("Failed to compress animated sticker below ${ANIMATED_STICKER_MAX_BYTES / 1024}KB")
    }

    private fun encodeAnimatedWebP(inputFile: File, config: CompressionConfig, isCropped: Boolean): File {
        val tempOutput = File.createTempFile("encoded_anim", ".webp")
        val frames = decodeFrames(inputFile)
        val processedFrames = processFrames(frames, config)
        
        try {
            val encoder = createEncoder(config.scale, config.scale)
            
            val webPConfig = com.aureusapps.android.webpandroid.encoder.WebPConfig(
                quality = config.quality.toFloat()
            )
            encoder.configure(webPConfig)
            
            var currentTimeMs = 0L
            processedFrames.forEach { frame ->
                val transformed = if (isCropped) centerCrop(frame.bitmap, config.scale) else resizeWithPadding(frame.bitmap, config.scale)
                encoder.addFrame(currentTimeMs, transformed)
                currentTimeMs += frame.durationMs.toLong()
                transformed.recycle()
            }
            
            // Log encoded metadata to verify against WhatsApp limits
            logger.d(TAG, "Encoded: frames=${processedFrames.size}, totalTime=${currentTimeMs}ms, avgDelay=${if(processedFrames.isNotEmpty()) currentTimeMs/processedFrames.size else 0}ms")
            
            val outputUri = android.net.Uri.fromFile(tempOutput)
            encoder.assemble(currentTimeMs, outputUri)
            encoder.release()
        } catch (e: Exception) {
            logger.e(TAG, "Error encoding animated WebP", e)
            val bitmap = frames.firstOrNull()?.bitmap ?: Bitmap.createBitmap(config.scale, config.scale, Bitmap.Config.ARGB_8888)
            val transformed = if (isCropped) centerCrop(bitmap, config.scale) else resizeWithPadding(bitmap, config.scale)
            saveAsWebP(transformed, tempOutput, ANIMATED_STICKER_MAX_BYTES)
        } finally {
            processedFrames.forEach { it.bitmap.recycle() }
        }
        
        return tempOutput
    }

    internal fun decodeFrames(file: File): List<Frame> {
        val frames = mutableListOf<Frame>()
        try {
            val bytes = file.readBytes()
            @Suppress("DEPRECATION")
            val movie = android.graphics.Movie.decodeByteArray(bytes, 0, bytes.size)
            
            if (movie != null && movie.duration() > 0) {
                val duration = movie.duration()
                val width = movie.width()
                val height = movie.height()
                logger.d(TAG, "Original GIF: duration=${duration}ms, size=${width}x$height")

                var currentTime = 0
                val sampleStep = 10 
                var lastBitmap: Bitmap? = null
                var lastFrameTime = 0
                
                while (currentTime < duration) {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    movie.setTime(currentTime)
                    movie.draw(canvas, 0f, 0f)
                    
                    if (lastBitmap == null || !bitmap.sameAs(lastBitmap)) {
                        if (lastBitmap != null) {
                            frames.add(Frame(lastBitmap, currentTime - lastFrameTime))
                        }
                        lastBitmap = bitmap
                        lastFrameTime = currentTime
                    } else {
                        bitmap.recycle()
                    }
                    currentTime += sampleStep
                }
                if (lastBitmap != null) {
                    frames.add(Frame(lastBitmap, duration - lastFrameTime))
                }
                logger.d(TAG, "Decoded: frames=${frames.size}, delays=${frames.map { it.durationMs }}")
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error decoding frames", e)
        }
        
        if (frames.isEmpty()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                frames.add(Frame(bitmap, 100))
                logger.d(TAG, "Decoded: fallback to static frame")
            }
        }
        return frames
    }

    private fun processFrames(frames: List<Frame>, config: CompressionConfig): List<Frame> {
        val minDelay = 80 // WhatsApp recommended minimum delay
        val maxDuration = 4800 // WhatsApp limit 5s (using 4.8s as safety)
        val maxFrames = 25 // WhatsApp safe limit (max is 50, but 25 is better for size/compat)
        
        var result = frames.filterIndexed { index, _ -> index % config.dropFramesRatio == 0 }
        
        // 1. Ensure minimum delay per frame
        result = result.map { frame ->
            if (frame.durationMs < minDelay) frame.copy(durationMs = minDelay) else frame
        }

        // 2. Limit frame count by sampling
        if (result.size > maxFrames) {
            val ratio = result.size.toDouble() / maxFrames
            val sampled = mutableListOf<Frame>()
            for (i in 0 until maxFrames) {
                val idx = (i * ratio).toInt().coerceAtMost(result.size - 1)
                sampled.add(result[idx])
            }
            result = sampled
        }

        // 3. Limit total duration
        var currentTotal = result.sumOf { it.durationMs }
        if (currentTotal > maxDuration) {
            val factor = maxDuration.toDouble() / currentTotal
            result = result.map { it.copy(durationMs = (it.durationMs * factor).toInt().coerceAtLeast(minDelay)) }
        }
        
        // Final duration check
        currentTotal = result.sumOf { it.durationMs }
        if (currentTotal > maxDuration) {
            // Force last reduction if still over
            val over = currentTotal - maxDuration
            val perFrame = (over / result.size) + 1
            result = result.map { it.copy(durationMs = (it.durationMs - perFrame).coerceAtLeast(minDelay)) }
        }

        return result
    }

    private fun saveAsPng(bitmap: Bitmap, outputFile: File): File {
        FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // For tray icons (PNG), we don't fall back to WebP to avoid extension/content mismatch.
        // A 96x96 PNG is highly unlikely to exceed 50KB.
        logger.d(TAG, "Saved tray icon: ${outputFile.length() / 1024} KB")
        bitmap.recycle()
        return outputFile
    }

    private fun centerCrop(source: Bitmap, targetSize: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val minEdge = minOf(sourceWidth, sourceHeight)
        
        val left = (sourceWidth - minEdge) / 2
        val top = (sourceHeight - minEdge) / 2
        
        val cropped = Bitmap.createBitmap(source, left, top, minEdge, minEdge)
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (cropped != source && cropped != scaled) cropped.recycle()
        return scaled
    }

    private fun resizeWithPadding(source: Bitmap, targetSize: Int): Bitmap {
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scale = minOf(targetSize.toFloat() / source.width, targetSize.toFloat() / source.height)
        val scaledW = (source.width * scale).toInt()
        val scaledH = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        canvas.drawBitmap(scaled, (targetSize - scaledW) / 2f, (targetSize - scaledH) / 2f, Paint(Paint.ANTI_ALIAS_FLAG))
        if (scaled != source) scaled.recycle()
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
