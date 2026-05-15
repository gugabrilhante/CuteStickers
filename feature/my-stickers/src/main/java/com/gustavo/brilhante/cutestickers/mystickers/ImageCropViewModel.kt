package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImageCropUiState {
    data object Loading : ImageCropUiState
    data class Ready(
        val displayBitmap: Bitmap,
        val mode: CropMode,
        val isAutoCutting: Boolean = false,
        val autoCutError: Boolean = false,
        val rotation: Float = 0f,
        val isCropping: Boolean = false
    ) : ImageCropUiState
}

sealed interface ImageCropUiEvent {
    data class LoadImage(val uri: Uri) : ImageCropUiEvent
    data class SetMode(val mode: CropMode) : ImageCropUiEvent
    data object Rotate : ImageCropUiEvent
    data class ConfirmCrop(
        val displayScale: Float,
        val displayOffset: Offset,
        val boxSize: Size,
        val cropSizePx: Float,
        val fitScale: Float
    ) : ImageCropUiEvent
}

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    private val processor: CropImageProcessor,
    private val autoCutProcessor: AutoCutProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImageCropUiState>(ImageCropUiState.Loading)
    val uiState: StateFlow<ImageCropUiState> = _uiState.asStateFlow()

    private val _cropResult = MutableSharedFlow<Uri>()
    val cropResult: SharedFlow<Uri> = _cropResult.asSharedFlow()

    private var originalBitmap: Bitmap? = null
    private var autoCutJob: Job? = null

    fun onEvent(event: ImageCropUiEvent) {
        when (event) {
            is ImageCropUiEvent.LoadImage -> loadImage(event.uri)
            is ImageCropUiEvent.SetMode -> setMode(event.mode)
            is ImageCropUiEvent.Rotate -> rotate()
            is ImageCropUiEvent.ConfirmCrop -> confirmCrop(event)
        }
    }

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImageCropUiState.Loading
            autoCutJob?.cancel()
            val bitmap = processor.loadBitmapWithExifCorrection(uri) ?: return@launch
            originalBitmap = bitmap
            _uiState.value = ImageCropUiState.Ready(displayBitmap = bitmap, mode = CropMode.ORIGINAL)
        }
    }

    private fun setMode(newMode: CropMode) {
        val current = _uiState.value as? ImageCropUiState.Ready ?: return
        if (newMode == current.mode) return
        val orig = originalBitmap ?: return

        autoCutJob?.cancel()

        when (newMode) {
            CropMode.ORIGINAL -> _uiState.value = current.copy(
                displayBitmap = orig,
                mode = CropMode.ORIGINAL,
                isAutoCutting = false,
                autoCutError = false
            )
            CropMode.AUTO_CUT -> {
                _uiState.value = current.copy(
                    mode = CropMode.AUTO_CUT,
                    isAutoCutting = true,
                    autoCutError = false
                )
                autoCutJob = viewModelScope.launch {
                    autoCutProcessor.removeBackground(orig)
                        .onSuccess { cutout ->
                            _uiState.update { s ->
                                (s as? ImageCropUiState.Ready)
                                    ?.copy(displayBitmap = cutout, isAutoCutting = false)
                                    ?: s
                            }
                        }
                        .onFailure {
                            _uiState.update { s ->
                                (s as? ImageCropUiState.Ready)
                                    ?.copy(displayBitmap = orig, isAutoCutting = false, autoCutError = true)
                                    ?: s
                            }
                        }
                }
            }
        }
    }

    private fun rotate() {
        _uiState.update { state ->
            if (state is ImageCropUiState.Ready) {
                state.copy(rotation = (state.rotation + 90f) % 360f)
            } else state
        }
    }

    private fun confirmCrop(event: ImageCropUiEvent.ConfirmCrop) {
        val current = _uiState.value as? ImageCropUiState.Ready ?: return
        if (current.isCropping || current.isAutoCutting) return

        _uiState.value = current.copy(isCropping = true)

        viewModelScope.launch {
            val bmp = current.displayBitmap
            val totalScale = event.displayScale * event.fitScale
            val rotatedW = if (current.rotation % 180 == 0f) bmp.width else bmp.height
            val rotatedH = if (current.rotation % 180 == 0f) bmp.height else bmp.width
            val imgW = rotatedW * totalScale
            val imgH = rotatedH * totalScale
            val imgLeft = (event.boxSize.width - imgW) / 2f + event.displayOffset.x
            val imgTop = (event.boxSize.height - imgH) / 2f + event.displayOffset.y
            val cropLeft = (event.boxSize.width - event.cropSizePx) / 2f
            val cropTop = (event.boxSize.height - event.cropSizePx) / 2f

            val (bmpX, bmpY, bmpSize) = computeCropRegion(
                cropLeft, cropTop, event.cropSizePx,
                imgLeft, imgTop, totalScale,
                rotatedW, rotatedH
            )

            val resultUri = processor.saveCroppedBitmap(bmp, bmpX, bmpY, bmpSize, current.rotation)
            _uiState.value = (uiState.value as? ImageCropUiState.Ready)?.copy(isCropping = false) ?: uiState.value
            _cropResult.emit(resultUri)
        }
    }
}
