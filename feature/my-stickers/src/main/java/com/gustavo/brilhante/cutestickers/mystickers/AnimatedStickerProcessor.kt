package com.gustavo.brilhante.cutestickers.mystickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimatedStickerProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val STICKER_SIZE = 512
        const val MAX_BYTES = 500 * 1024 // WhatsApp limit for animated stickers is 500KB
        const val MIN_FRAME_DELAY_MS = 40 // Target ~25 FPS
        const val MAX_DURATION_MS = 5000
    }

    fun encodeToWebP(frames: List<Frame>, outputFile: File): Result<File> = runCatching {
        val encoder = WebPAnimEncoder(context, STICKER_SIZE, STICKER_SIZE)
        val config = WebPConfig(quality = 75f)
        encoder.configure(config)

        var currentTimeMs = 0L
        frames.forEach { frame ->
            if (!frame.bitmap.isRecycled) {
                val processed = processBitmap(frame.bitmap)
                encoder.addFrame(currentTimeMs, processed)
                currentTimeMs += frame.durationMs.coerceAtLeast(MIN_FRAME_DELAY_MS).toLong()
                // Always recycle the 'processed' bitmap as it was created in processBitmap
                processed.recycle()
            }
        }

        encoder.assemble(currentTimeMs, Uri.fromFile(outputFile))
        encoder.release()
        outputFile
    }

    private fun processBitmap(source: Bitmap): Bitmap {
        if (source.isRecycled) {
            // Fallback for safety, though we should avoid getting here
            return Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        }
        val result = Bitmap.createBitmap(STICKER_SIZE, STICKER_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Calculate scale to fit while preserving aspect ratio
        val scale = minOf(STICKER_SIZE.toFloat() / source.width, STICKER_SIZE.toFloat() / source.height)
        val scaledW = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(1)
        
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        
        // Center the scaled bitmap in the 512x512 canvas
        val left = (STICKER_SIZE - scaledW) / 2f
        val top = (STICKER_SIZE - scaledH) / 2f
        
        canvas.drawBitmap(scaled, left, top, Paint(Paint.ANTI_ALIAS_FLAG))

        if (scaled != source) {
            scaled.recycle()
        }
        return result
    }

    data class Frame(val bitmap: Bitmap, val durationMs: Int)
}
