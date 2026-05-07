package com.gustavo.brilhante.cutecats.feature.mediadetails

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutecats.feature.stickers.domain.CreateStickerUseCase
import com.gustavo.brilhante.cutecats.feature.stickers.domain.ExportStickerPackUseCase
import com.gustavo.brilhante.cutecats.feature.stickers.domain.SaveMediaUseCase
import com.gustavo.brilhante.cutecats.feature.stickers.domain.StickerPack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StickerState {
    data object Idle : StickerState
    data object Loading : StickerState
    data class Success(val pack: StickerPack) : StickerState
    data class Error(val message: String) : StickerState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Loading : DownloadState
    data object Success : DownloadState
    data class Error(val message: String) : DownloadState
}

data class MediaDetailsUiState(
    val imageUrl: String = "",
    val mediaId: String = "",
    val stickerState: StickerState = StickerState.Idle,
    val downloadState: DownloadState = DownloadState.Idle
)

sealed interface MediaDetailsEvent {
    data class LaunchIntent(val intent: Intent) : MediaDetailsEvent
}

@HiltViewModel
class MediaDetailsViewModel @Inject constructor(
    private val createStickerUseCase: CreateStickerUseCase,
    private val exportStickerPackUseCase: ExportStickerPackUseCase,
    private val saveMediaUseCase: SaveMediaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<MediaDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun init(imageUrl: String, mediaId: String) {
        _uiState.update { it.copy(imageUrl = imageUrl, mediaId = mediaId) }
    }

    fun onAddToWhatsApp() {
        val state = _uiState.value
        if (state.stickerState is StickerState.Loading) return
        _uiState.update { it.copy(stickerState = StickerState.Loading) }
        viewModelScope.launch {
            createStickerUseCase(state.imageUrl, state.mediaId)
                .onSuccess { pack ->
                    _uiState.update { it.copy(stickerState = StickerState.Success(pack)) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(stickerState = StickerState.Error(e.message ?: "Failed to create sticker"))
                    }
                }
        }
    }

    fun onConfirmExport(pack: StickerPack) {
        viewModelScope.launch {
            exportStickerPackUseCase.buildIntent(pack)
                .onSuccess { intent ->
                    _events.send(MediaDetailsEvent.LaunchIntent(intent))
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(stickerState = StickerState.Error(e.message ?: "WhatsApp not available"))
                    }
                }
        }
    }

    fun DismissStickerSheet() {
        _uiState.update { it.copy(stickerState = StickerState.Idle) }
    }

    fun onDownloadMedia() {
        val imageUrl = _uiState.value.imageUrl
        if (_uiState.value.downloadState is DownloadState.Loading) return
        _uiState.update { it.copy(downloadState = DownloadState.Loading) }
        viewModelScope.launch {
            saveMediaUseCase(imageUrl)
                .onSuccess { _uiState.update { it.copy(downloadState = DownloadState.Success) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(downloadState = DownloadState.Error(e.message ?: "Download failed"))
                    }
                }
        }
    }

    fun onDownloadPermissionDenied() {
        _uiState.update { it.copy(downloadState = DownloadState.Error("Storage permission required to save image")) }
    }
}
