package com.gustavo.brilhante.cutestickers.mystickers.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.mystickers.CropImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

internal class CropImageProcessorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider
) : CropImageProcessor {

    override suspend fun loadBitmapWithExifCorrection(uri: Uri): Bitmap? =
        withContext(ioDispatcher) {
            val raw = openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext null
            val degrees = openInputStream(uri)?.use { ExifInterface(it) }?.let { exif ->
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            if (degrees == 0f) return@withContext raw
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                .also { raw.recycle() }
        }

    override suspend fun saveCroppedBitmap(
        source: Bitmap,
        x: Int,
        y: Int,
        size: Int,
        rotation: Float
    ): Uri = withContext(ioDispatcher) {
        val matrix = Matrix().apply { postRotate(rotation) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        val cropped = Bitmap.createBitmap(rotated, x, y, size, size)
        val transparent = source.hasAlpha()
        val ext = if (transparent) "png" else "jpg"
        val format = if (transparent) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val f = File(context.cacheDir, "crop_${timeProvider.getCurrentTimeMillis()}.$ext")
        FileOutputStream(f).use { cropped.compress(format, if (transparent) 0 else 95, it) }
        if (rotated !== source) rotated.recycle()
        cropped.recycle()
        Uri.fromFile(f)
    }

    private fun openInputStream(uri: Uri): InputStream? = when (uri.scheme) {
        "file" -> File(uri.path!!).inputStream()
        else -> context.contentResolver.openInputStream(uri)
    }
}
