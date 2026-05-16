package com.gustavo.brilhante.cutestickers.mystickers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.gustavo.brilhante.cutestickers.common.network.CatsDispatchers
import com.gustavo.brilhante.cutestickers.common.network.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoImportProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(CatsDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun extractFrames(
        videoUri: Uri,
        startTimeMs: Long,
        endTimeMs: Long,
        maxFrames: Int = 80,
        cropRect: android.graphics.RectF? = null
    ): List<AnimatedStickerProcessor.Frame> = withContext(ioDispatcher) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = (endTimeMs - startTimeMs).coerceAtLeast(100)
            
            val targetFps = 20
            val targetInterval = (1000 / targetFps).toLong()
            val frameIntervalMs = targetInterval.coerceAtLeast(AnimatedStickerProcessor.MIN_FRAME_DELAY_MS.toLong())
            
            val actualFramesCount = (durationMs / frameIntervalMs).toInt().coerceAtMost(maxFrames)
            
            (0 until actualFramesCount).mapNotNull { i ->
                val timeUs = (startTimeMs + i * frameIntervalMs) * 1000
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) 
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                bitmap?.let { source ->
                    val finalBitmap = if (cropRect != null) {
                        val left = (source.width * cropRect.left).toInt()
                        val top = (source.height * cropRect.top).toInt()
                        val width = (source.width * cropRect.width()).toInt()
                        val height = (source.height * cropRect.height()).toInt()
                        val cropped = android.graphics.Bitmap.createBitmap(source, left, top, width, height)
                        if (cropped != source) {
                            source.recycle()
                        }
                        cropped
                    } else {
                        source
                    }
                    AnimatedStickerProcessor.Frame(finalBitmap, frameIntervalMs.toInt())
                }
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }
}
