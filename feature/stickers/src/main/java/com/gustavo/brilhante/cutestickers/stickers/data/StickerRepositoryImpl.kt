package com.gustavo.brilhante.cutestickers.stickers.data

import com.gustavo.brilhante.cutestickers.common.TimeProvider
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import com.gustavo.brilhante.cutestickers.model.MediaType
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerItem
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StickerRepositoryImpl @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val fileManager: StickerFileManager,
    private val stickerStore: StickerStore,
    private val galleryDataSource: GalleryDataSource,
    private val timeProvider: TimeProvider,
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
            imageProcessor.downloadAndProcess(imageUrl, trayFile, ImageProcessor.TRAY_SIZE)
                .getOrThrow()

            val existingPack = stickerStore.loadAllPacks().find { it.id == packId }
            val existingStickers = existingPack?.stickers ?: emptyList()
            
            val newStickerInfo = StickerInfo(imageFileName = stickerFileName)
            val updatedStickers = (existingStickers.filter { it.imageFileName != stickerFileName } + newStickerInfo)
                .takeLast(30)

            val packInfo = StickerPackInfo(
                id = packId,
                name = packName,
                publisher = "CuteStickers",
                trayImageFileName = trayFile.name,
                stickers = updatedStickers,
                isAnimated = isAnimated
            )
            stickerStore.savePack(packInfo)

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
                val currentTime = timeProvider.getCurrentTimeMillis()
                val fileName = if (isGif) "cutesticker_$currentTime.gif" else "cutesticker_$currentTime.jpg"
                val mimeType = if (isGif) "image/gif" else "image/jpeg"
                galleryDataSource.saveImage(bytes, fileName, mimeType).getOrThrow()
            }
        }
}
