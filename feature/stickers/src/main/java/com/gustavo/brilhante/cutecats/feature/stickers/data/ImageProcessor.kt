package com.gustavo.brilhante.cutecats.feature.stickers.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ImageProcessor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        const val STICKER_SIZE = 512
        const val TRAY_SIZE = 96
        private const val STICKER_MAX_BYTES = 100 * 1024   // WhatsApp limit: 100 KB
        private const val TRAY_MAX_BYTES = 50 * 1024       // WhatsApp limit: 50 KB
        private val QUALITY_STEPS = intArrayOf(80, 60, 40, 20)
    }

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

    private fun saveAsPng(bitmap: Bitmap, outputFile: File, maxBytes: Int): File {
        FileOutputStream(outputFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return outputFile
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
        source.recycle()
        return result
    }

    /**
     * Compresses bitmap as lossy WebP, stepping down quality until the file fits within
     * maxBytes. Lossy is required because lossless WebP of a 512×512 photo easily exceeds
     * the 100 KB WhatsApp limit.
     */
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
