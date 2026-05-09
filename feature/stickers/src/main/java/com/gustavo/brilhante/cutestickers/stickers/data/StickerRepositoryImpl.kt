package com.gustavo.brilhante.cutestickers.stickers.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerItem
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerRepository
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
        mediaId: String,
        mediaType: MediaType
    ): Result<StickerPack> = withContext(ioDispatcher) {
        runCatching {
            val isAnimated = mediaType is MediaType.Animated
            // Use a fixed packId to consolidate all stickers into a single pack per type (animated or static).
            // WhatsApp allows up to 30 stickers per pack.
            val packId = if (isAnimated) "cute_stickers_animated" else "cute_stickers_static"
            val packName = if (isAnimated) "Cute Animated Stickers" else "Cute Stickers"

            val stickerFileName = "${mediaId}.webp"
            val primaryFile = File(fileManager.packDir(packId), stickerFileName)
            
            if (isAnimated) {
                imageProcessor.downloadAndProcessAnimated(imageUrl, primaryFile)
                    .getOrThrow()
            } else {
                imageProcessor.downloadAndProcess(imageUrl, primaryFile, ImageProcessor.STICKER_SIZE)
                    .getOrThrow()
            }

            val trayFile = File(fileManager.packDir(packId), "tray_icon.png")
            // Always update tray icon to match the latest sticker added
            imageProcessor.downloadAndProcess(imageUrl, trayFile, ImageProcessor.TRAY_SIZE)
                .getOrThrow()

            // Load existing pack info if it exists to append the new sticker
            val existingPack = StickerStore(context).loadAllPacks().find { it.id == packId }
            val existingStickers = existingPack?.stickers ?: emptyList()
            
            // Avoid duplicates if the same mediaId is added again
            val newStickerInfo = StickerInfo(imageFileName = stickerFileName)
            val updatedStickers = (existingStickers.filter { it.imageFileName != stickerFileName } + newStickerInfo)
                .takeLast(30) // WhatsApp limit is 30 stickers

            val packInfo = StickerPackInfo(
                id = packId,
                name = packName,
                publisher = "CuteStickers",
                trayImageFileName = trayFile.name,
                stickers = updatedStickers,
                isAnimated = isAnimated
            )
            StickerStore(context).savePack(packInfo)

            StickerPack(
                id = packId,
                name = packInfo.name,
                publisher = packInfo.publisher,
                trayImageFileName = packInfo.trayImageFileName,
                stickers = updatedStickers.map { StickerItem(imageFileName = it.imageFileName, emojis = listOf("😊")) },
                isAnimated = isAnimated
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
                val isGif = imageUrl.lowercase().endsWith(".gif")
                val fileName = if (isGif) "cutesticker_${System.currentTimeMillis()}.gif" else "cutesticker_${System.currentTimeMillis()}.jpg"
                val mimeType = if (isGif) "image/gif" else "image/jpeg"
                writeToGallery(bytes, fileName, mimeType)
            }
        }

    private fun writeToGallery(bytes: ByteArray, fileName: String, mimeType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
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
