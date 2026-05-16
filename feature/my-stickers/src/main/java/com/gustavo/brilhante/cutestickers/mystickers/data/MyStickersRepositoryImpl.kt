package com.gustavo.brilhante.cutestickers.mystickers.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.mystickers.domain.MySticker
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.mystickers.domain.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MyStickersRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: MyStickerDao,
    private val timeProvider: TimeProvider,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient
) : MyStickersRepository {

    private val stickersDir: File
        get() = File(context.filesDir, "my-stickers").also { it.mkdirs() }

    override fun getStickers(): Flow<List<MySticker>> =
        dao.getStickers().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveFromUri(uriString: String, mediaType: MediaType): Result<MySticker> =
        withContext(ioDispatcher) {
            runCatching {
                val id = UUID.randomUUID().toString()
                // Preserve PNG/WebP extension so transparency (Alpha channel) is not lost on re-encode
                val isPng = uriString.endsWith(".png", ignoreCase = true)
                val isWebp = uriString.endsWith(".webp", ignoreCase = true)
                val ext = when {
                    isPng -> "png"
                    isWebp -> "webp"
                    else -> "jpg"
                }
                val file = File(stickersDir, "$id.$ext")

                val bytes = when {
                    uriString.startsWith("file://") -> File(uriString.removePrefix("file://")).readBytes()
                    uriString.startsWith("/") -> File(uriString).readBytes()
                    else -> {
                        val uri = Uri.parse(uriString)
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: error("Failed to open URI: $uriString")
                    }
                }

                // Skip EXIF normalization for PNG/WebP files
                file.writeBytes(if (isPng || isWebp) bytes else normalizeExifOrientation(bytes))

                val sticker = MySticker(
                    id = id,
                    localPath = file.absolutePath,
                    sourceType = SourceType.GALLERY,
                    createdAt = timeProvider.getCurrentTimeMillis(),
                    mediaType = mediaType
                )
                dao.insert(sticker.toEntity())
                sticker
            }
        }

    override suspend fun saveFromUrl(imageUrl: String, mediaId: String): Result<MySticker> =
        withContext(ioDispatcher) {
            runCatching {
                val bytes = downloadBytes(imageUrl)
                val id = mediaId.ifBlank { UUID.randomUUID().toString() }
                val ext = if (imageUrl.lowercase().endsWith(".gif")) "gif" else "jpg"
                val file = File(stickersDir, "$id.$ext")
                file.writeBytes(bytes)

                val sticker = MySticker(
                    id = id,
                    localPath = file.absolutePath,
                    sourceType = SourceType.APP,
                    createdAt = timeProvider.getCurrentTimeMillis()
                )
                dao.insert(sticker.toEntity())
                sticker
            }
        }

    override suspend fun deleteSticker(id: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val entity = dao.getById(id)
                dao.deleteById(id)
                entity?.localPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) file.delete()
                }
                Unit
            }
        }

    private fun downloadBytes(url: String): ByteArray {
        if (url.startsWith("/") || url.startsWith("file://")) {
            val path = url.removePrefix("file://")
            return File(path).readBytes()
        }
        val request = Request.Builder().url(url).build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Failed to download: ${response.code}")
            response.body?.bytes() ?: error("Empty response body")
        }
    }

    private fun normalizeExifOrientation(bytes: ByteArray): ByteArray {
        val rotation = bytes.inputStream().use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
        if (rotation == 0f) return bytes

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
        val rotated = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().also { it.postRotate(rotation) }, true
        )
        bitmap.recycle()

        return ByteArrayOutputStream().use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
            rotated.recycle()
            out.toByteArray()
        }
    }
}
