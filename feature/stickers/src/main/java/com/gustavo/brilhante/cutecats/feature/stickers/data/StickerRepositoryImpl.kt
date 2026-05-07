package com.gustavo.brilhante.cutecats.feature.stickers.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "StickerRepository"

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
            // Using a new ID prefix to ensure WhatsApp treats it as a fresh pack
            val packId = "cat_$mediaId" 
            val shortId = if (mediaId.length > 4) mediaId.takeLast(4) else mediaId
            val packName = "CuteCat $shortId"
            Log.d(TAG, "createSticker — packId=$packId, packName=$packName")

            // Download and process the sticker image once (512×512, ≤ 100 KB WebP)
            val primaryFile = fileManager.stickerFile(packId, mediaId)
            imageProcessor.downloadAndProcess(imageUrl, primaryFile, ImageProcessor.STICKER_SIZE)
                .getOrThrow()
            Log.d(TAG, "primary sticker written — ${primaryFile.absolutePath} (${primaryFile.length()}B)")

            // WhatsApp enforces a minimum of 3 stickers per pack.
            // Reuse the same image rather than downloading it again.
            val copy2 = fileManager.stickerFile(packId, "${mediaId}_2")
            val copy3 = fileManager.stickerFile(packId, "${mediaId}_3")
            primaryFile.copyTo(copy2, overwrite = true)
            primaryFile.copyTo(copy3, overwrite = true)
            Log.d(TAG, "sticker copies written — ${copy2.name}, ${copy3.name}")

            // Tray icon (96×96, ≤ 50 KB WebP) shown in WhatsApp sticker picker
            val trayFile = fileManager.trayIconFile(packId)
            imageProcessor.downloadAndProcess(imageUrl, trayFile, ImageProcessor.TRAY_SIZE)
                .getOrThrow()
            Log.d(TAG, "tray icon written — ${trayFile.absolutePath} (${trayFile.length()}B)")

            val stickers = listOf(
                StickerInfo(imageFileName = primaryFile.name),
                StickerInfo(imageFileName = copy2.name),
                StickerInfo(imageFileName = copy3.name)
            )
            Log.d(TAG, "preparing StickerPackInfo with ${stickers.size} stickers")
            val packInfo = StickerPackInfo(
                id = packId,
                name = packName,
                publisher = "CuteCats",
                trayImageFileName = trayFile.name,
                stickers = stickers
            )
            StickerStore(context).savePack(packInfo)
            Log.d(TAG, "pack info saved successfully for packId: $packId")

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
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
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
