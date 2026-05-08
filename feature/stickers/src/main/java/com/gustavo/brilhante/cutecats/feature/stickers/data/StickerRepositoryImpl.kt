package com.gustavo.brilhante.cutecats.feature.stickers.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gustavo.brilhante.cutecats.core.common.network.CatsDispatchers
import com.gustavo.brilhante.cutecats.core.common.network.Dispatcher
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerItem
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerPack
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StickerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageProcessor: ImageProcessor,
    private val fileManager: StickerFileManager,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient
) : StickerRepository {

    override suspend fun createStickerFromUrl(
        imageUrl: String,
        mediaId: String
    ): Result<StickerPack> = withContext(ioDispatcher) {
        runCatching {
            // Use a unique packId per export to ensure WhatsApp treats it as a new pack
            val packId = "cat_${mediaId}_${System.currentTimeMillis()}"
            val shortId = if (mediaId.length > 4) mediaId.takeLast(4) else mediaId
            val packName = "CuteCat $shortId"

            val primaryFile = fileManager.stickerFile(packId, mediaId)
            imageProcessor.downloadAndProcess(imageUrl, primaryFile, ImageProcessor.STICKER_SIZE)
                .getOrThrow()

            val copy2 = fileManager.stickerFile(packId, "${mediaId}_2")
            val copy3 = fileManager.stickerFile(packId, "${mediaId}_3")
            primaryFile.copyTo(copy2, overwrite = true)
            primaryFile.copyTo(copy3, overwrite = true)

            val trayFile = fileManager.trayIconFile(packId)
            imageProcessor.downloadAndProcess(imageUrl, trayFile, ImageProcessor.TRAY_SIZE)
                .getOrThrow()

            val stickers = listOf(
                StickerInfo(imageFileName = primaryFile.name),
                StickerInfo(imageFileName = copy2.name),
                StickerInfo(imageFileName = copy3.name)
            )
            val packInfo = StickerPackInfo(
                id = packId,
                name = packName,
                publisher = "CuteCats",
                trayImageFileName = trayFile.name,
                stickers = stickers
            )
            StickerStore(context).savePack(packInfo)

            StickerPack(
                id = packId,
                name = packInfo.name,
                publisher = packInfo.publisher,
                trayImageFileName = packInfo.trayImageFileName,
                stickers = stickers.map { StickerItem(imageFileName = it.imageFileName, emojis = listOf("😊")) }
            )
        }
    }

    override suspend fun saveMediaToGallery(imageUrl: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val request = Request.Builder().url(imageUrl).build()
                val bytes = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Failed to download image: ${response.code} ${response.message}")
                    response.body?.bytes() ?: error("Empty image response")
                }
                writeToGallery(bytes, "cutesticker_${System.currentTimeMillis()}.jpg")
            }
        }

    private fun writeToGallery(bytes: ByteArray, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/CuteStickers")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Failed to insert image into MediaStore")

            try {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream == null) {
                    resolver.delete(uri, null, null)
                    throw IOException("Failed to open output stream for $uri")
                }

                outputStream.use { it.write(bytes) }

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
