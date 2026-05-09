package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface GalleryDataSource {
    suspend fun saveImage(bytes: ByteArray, fileName: String, mimeType: String): Result<Unit>
}

@Singleton
class GalleryDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GalleryDataSource {

    override suspend fun saveImage(bytes: ByteArray, fileName: String, mimeType: String): Result<Unit> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CuteStickers")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to insert image into MediaStore")

            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IOException("Failed to open output stream for $uri")

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            val dir = File(
                @Suppress("DEPRECATION")
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "CuteStickers"
            ).also { it.mkdirs() }
            File(dir, fileName).writeBytes(bytes)
        }
    }
}
