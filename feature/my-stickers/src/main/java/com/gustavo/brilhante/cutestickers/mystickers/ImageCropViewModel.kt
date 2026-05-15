package com.gustavo.brilhante.cutestickers.mystickers

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        val autoCutError: Boolean = false
    ) : ImageCropUiState
}

@HiltViewModel
class ImageCropViewModel @Inject constructor(
    private val processor: CropImageProcessor,
    private val autoCutProcessor: AutoCutProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImageCropUiState>(ImageCropUiState.Loading)
    val uiState: StateFlow<ImageCropUiState> = _uiState.asStateFlow()

    private var originalBitmap: Bitmap? = null
    private var autoCutJob: Job? = null

    fun loadImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ImageCropUiState.Loading
            autoCutJob?.cancel()
            val bitmap = processor.loadBitmapWithExifCorrection(uri) ?: return@launch
            originalBitmap = bitmap
            _uiState.value = ImageCropUiState.Ready(displayBitmap = bitmap, mode = CropMode.ORIGINAL)
        }
    }

    fun setMode(newMode: CropMode) {
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

    suspend fun saveCrop(bmp: Bitmap, x: Int, y: Int, size: Int, rotation: Float): Uri =
        processor.saveCroppedBitmap(bmp, x, y, size, rotation)
}
