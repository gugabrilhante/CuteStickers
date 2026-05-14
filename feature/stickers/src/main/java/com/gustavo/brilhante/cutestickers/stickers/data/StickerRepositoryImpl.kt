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

    init {
        stickerStore.migrateIfNeeded()
    }

    override suspend fun createStickerFromUrl(
        imageUrl: String,
        mediaId: String,
        mediaType: MediaType,
        isCropped: Boolean
    ): Result<StickerPack> = withContext(ioDispatcher) {
        runCatching {
            val isAnimated = mediaType is MediaType.Animated
            val packId = if (isAnimated) "anim_pack" else "static_pack"
            val packName = if (isAnimated) "Cute Animated" else "Cute Static"

            val stickerFileName = "${mediaId}.webp"
            val primaryFile = File(fileManager.packDir(packId), stickerFileName)
            
            if (isAnimated) {
                imageProcessor.downloadAndProcessAnimated(imageUrl, primaryFile, isCropped)
                    .getOrThrow()
            } else {
                imageProcessor.downloadAndProcess(imageUrl, primaryFile, ImageProcessor.STICKER_SIZE, isCropped)
                    .getOrThrow()
            }

            val trayFile = File(fileManager.packDir(packId), "tray_icon.webp")
            imageProcessor.downloadAndProcess(imageUrl, trayFile, ImageProcessor.TRAY_SIZE, isCropped)
                .getOrThrow()

            val existingPack = stickerStore.loadAllPacks().find { it.id == packId }
            val existingStickers = existingPack?.stickers ?: emptyList()
            
            val newStickerInfo = StickerInfo(imageFileName = stickerFileName)
            var updatedStickers = (existingStickers.filter { 
                !it.imageFileName.startsWith("placeholder_") && it.imageFileName != stickerFileName 
            } + newStickerInfo).takeLast(30)

            // WhatsApp REQUIRES at least 3 stickers in a pack to be valid.
            // If we have fewer, we duplicate the file to meet the requirement.
            if (updatedStickers.isNotEmpty() && updatedStickers.size < 3) {
                val lastInfo = updatedStickers.last()
                val lastFile = File(fileManager.packDir(packId), lastInfo.imageFileName)
                
                val placeholders = mutableListOf<StickerInfo>()
                for (i in 1..(3 - updatedStickers.size)) {
                    val placeholderFileName = "placeholder_${i}.webp"
                    val placeholderFile = File(fileManager.packDir(packId), placeholderFileName)
                    lastFile.copyTo(placeholderFile, overwrite = true)
                    placeholders.add(StickerInfo(imageFileName = placeholderFileName))
                }
                updatedStickers = updatedStickers + placeholders
            }

            val packInfo = StickerPackInfo(
                id = packId,
                name = packName,
                publisher = "CuteStickers",
                trayImageFileName = trayFile.name,
                stickers = updatedStickers,
                isAnimated = isAnimated,
                version = timeProvider.getCurrentTimeMillis().toString()
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
