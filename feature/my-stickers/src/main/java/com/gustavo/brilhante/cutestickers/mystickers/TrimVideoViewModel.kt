package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TrimVideoUiState(
    val videoUri: Uri? = null,
    val durationMs: Long = 0,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = 0,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isDurationValid: Boolean = true,
    val thumbnails: List<Bitmap> = emptyList(),
    val cropRect: android.graphics.RectF = android.graphics.RectF(0f, 0f, 1f, 1f),
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isSquareCrop: Boolean = false
)

@HiltViewModel
class TrimVideoViewModel @Inject constructor(
    private val videoImportProcessor: VideoImportProcessor,
    private val animatedStickerProcessor: AnimatedStickerProcessor,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrimVideoUiState())
    val uiState = _uiState.asStateFlow()

    fun loadVideo(uri: Uri) {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            var width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
            var height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
            val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0
            
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            _uiState.update { 
                it.copy(
                    videoUri = uri, 
                    durationMs = duration,
                    startTimeMs = 0,
                    endTimeMs = minOf(duration, AnimatedStickerProcessor.MAX_DURATION_MS.toLong()),
                    videoWidth = width,
                    videoHeight = height,
                    cropRect = getDefaultCropRect(width, height)
                ) 
            }
            loadThumbnails(uri, duration)
            validateDuration()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Failed to load video") }
        } finally {
            retriever.release()
        }
    }

    private fun getDefaultCropRect(width: Int, height: Int): android.graphics.RectF {
        if (width == 0 || height == 0) return android.graphics.RectF(0f, 0f, 1f, 1f)
        // Start with a 1:1 square in the center
        return if (width > height) {
            val side = height.toFloat() / width
            val left = (1f - side) / 2f
            android.graphics.RectF(left, 0f, left + side, 1f)
        } else {
            val side = width.toFloat() / height
            val top = (1f - side) / 2f
            android.graphics.RectF(0f, top, 1f, top + side)
        }
    }

    fun onTrimChanged(start: Long, end: Long) {
        _uiState.update { it.copy(startTimeMs = start, endTimeMs = end) }
        validateDuration()
    }

    fun onCropChanged(rect: android.graphics.RectF) {
        _uiState.update { it.copy(cropRect = rect) }
    }

    fun toggleSquareCrop() {
        _uiState.update { state ->
            val isSquare = !state.isSquareCrop
            if (isSquare) {
                val currentRect = state.cropRect
                val videoWidth = state.videoWidth.toFloat()
                val videoHeight = state.videoHeight.toFloat()
                
                if (videoWidth == 0f || videoHeight == 0f) return@update state.copy(isSquareCrop = isSquare)

                val centerH = currentRect.centerX()
                val centerV = currentRect.centerY()
                
                // Try to keep the same area or at least a reasonable size
                var newWidth = currentRect.width()
                var newHeight = newWidth * videoWidth / videoHeight
                
                if (newHeight > 1f) {
                    newHeight = currentRect.height()
                    newWidth = newHeight * videoHeight / videoWidth
                }
                
                if (newWidth > 1f) { // Should not happen if videoWidth/videoHeight is correct
                     newWidth = 1f
                     newHeight = newWidth * videoWidth / videoHeight
                }

                val left = (centerH - newWidth / 2).coerceIn(0f, 1f - newWidth)
                val top = (centerV - newHeight / 2).coerceIn(0f, 1f - newHeight)
                
                state.copy(
                    isSquareCrop = isSquare,
                    cropRect = android.graphics.RectF(left, top, left + newWidth, top + newHeight)
                )
            } else {
                // When disabling square crop, we keep the current rect but allow free movement
                state.copy(isSquareCrop = isSquare)
            }
        }
    }

    private fun loadThumbnails(uri: Uri, duration: Long) {
        viewModelScope.launch {
            val thumbnails = mutableListOf<Bitmap>()
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val count = 8
                val interval = duration / count
                for (i in 0 until count) {
                    val timeUs = (i * interval) * 1000
                    retriever.getFrameAtTime(timeUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let {
                        thumbnails.add(it)
                    }
                }
                _uiState.update { it.copy(thumbnails = thumbnails) }
            } catch (e: Exception) {
                // Ignore
            } finally {
                retriever.release()
            }
        }
    }

    private fun validateDuration() {
        val state = _uiState.value
        val duration = state.endTimeMs - state.startTimeMs
        val isValid = duration in 100..AnimatedStickerProcessor.MAX_DURATION_MS
        _uiState.update { it.copy(isDurationValid = isValid) }
    }

    fun processVideo(onComplete: (Uri) -> Unit) {
        val state = _uiState.value
        if (!state.isDurationValid || state.videoUri == null) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            val frames = videoImportProcessor.extractFrames(
                state.videoUri,
                state.startTimeMs,
                state.endTimeMs,
                cropRect = state.cropRect
            )
            
            if (frames.isEmpty()) {
                _uiState.update { it.copy(isProcessing = false, error = "Failed to extract frames") }
                return@launch
            }

            val outputFile = File(context.cacheDir, "trimmed_sticker_${System.currentTimeMillis()}.webp")
            animatedStickerProcessor.encodeToWebP(frames, outputFile)
                .onSuccess { file ->
                    onComplete(Uri.fromFile(file))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isProcessing = false, error = e.message ?: "Failed to encode sticker") }
                }
        }
    }
}
