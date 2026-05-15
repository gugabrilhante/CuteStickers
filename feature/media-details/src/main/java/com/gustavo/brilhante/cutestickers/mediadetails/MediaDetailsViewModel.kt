package com.gustavo.brilhante.cutestickers.mediadetails

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutestickers.common.MediaMetadataResolver
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.stickers.domain.CreateStickerUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.ExportStickerPackUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.SaveMediaUseCase
import com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack
import com.gustavo.brilhante.cutestickers.model.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface AutoCutState {
    data object Idle : AutoCutState
    data object Loading : AutoCutState
    data class Ready(val localUri: String) : AutoCutState
    data object Error : AutoCutState
}

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

sealed interface SaveToMyStickersState {
    data object Idle : SaveToMyStickersState
    data object Loading : SaveToMyStickersState
    data object Success : SaveToMyStickersState
    data class Error(val message: String) : SaveToMyStickersState
}

data class MediaDetailsUiState(
    val imageUrl: String = "",
    val mediaId: String = "",
    val mediaType: MediaType = MediaType.Static,
    val stickerState: StickerState = StickerState.Idle,
    val downloadState: DownloadState = DownloadState.Idle,
    val saveToMyStickersState: SaveToMyStickersState = SaveToMyStickersState.Idle,
    val autoCutState: AutoCutState = AutoCutState.Idle,
    val isCropped: Boolean = true,
    val isLocalMedia: Boolean = false
)

sealed interface MediaDetailsEvent {
    data class ExportToWhatsApp(
        val packId: String,
        val authority: String,
        val packName: String,
        val targetPackage: String
    ) : MediaDetailsEvent
}

@HiltViewModel
class MediaDetailsViewModel @Inject constructor(
    private val createStickerUseCase: CreateStickerUseCase,
    private val exportStickerPackUseCase: ExportStickerPackUseCase,
    private val saveMediaUseCase: SaveMediaUseCase,
    private val metadataResolver: MediaMetadataResolver,
    private val myStickersRepository: MyStickersRepository,
    private val autoCutUseCase: AutoCutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<MediaDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun init(imageUrl: String, mediaId: String) {
        val type = metadataResolver.getMediaType(imageUrl)
        val isLocal = imageUrl.startsWith("file://") || imageUrl.startsWith("/")
        _uiState.update { it.copy(imageUrl = imageUrl, mediaId = mediaId, mediaType = type, isLocalMedia = isLocal) }
    }

    fun onToggleCrop() {
        _uiState.update { it.copy(isCropped = !it.isCropped) }
    }

    fun onToggleAutoCut() {
        val state = _uiState.value
        when (state.autoCutState) {
            is AutoCutState.Loading -> return
            is AutoCutState.Ready -> {
                deleteTempFile(state.autoCutState.localUri)
                _uiState.update { it.copy(autoCutState = AutoCutState.Idle) }
            }
            is AutoCutState.Idle, is AutoCutState.Error -> {
                _uiState.update { it.copy(autoCutState = AutoCutState.Loading) }
                viewModelScope.launch {
                    autoCutUseCase(state.imageUrl)
                        .onSuccess { file ->
                            _uiState.update {
                                it.copy(autoCutState = AutoCutState.Ready("file://${file.absolutePath}"))
                            }
                        }
                        .onFailure {
                            _uiState.update { it.copy(autoCutState = AutoCutState.Error) }
                        }
                }
            }
        }
    }

    fun onAddToWhatsApp() {
        val state = _uiState.value
        if (state.stickerState is StickerState.Loading) return
        _uiState.update { it.copy(stickerState = StickerState.Loading) }
        viewModelScope.launch {
            val imageUrl = if (state.autoCutState is AutoCutState.Ready) {
                state.autoCutState.localUri
            } else {
                state.imageUrl
            }
            createStickerUseCase(imageUrl, state.mediaId, state.mediaType, state.isCropped)
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
            exportStickerPackUseCase.getExportMetadata(pack)
                .onSuccess { metadata ->
                    _events.send(
                        MediaDetailsEvent.ExportToWhatsApp(
                            packId = metadata.packId,
                            authority = metadata.authority,
                            packName = metadata.packName,
                            targetPackage = metadata.targetPackage
                        )
                    )
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(stickerState = StickerState.Error(e.message ?: "Export failed"))
                    }
                }
        }
    }

    fun onExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            _uiState.update { it.copy(stickerState = StickerState.Idle) }
        } else {
            val errorMessage = data?.getStringExtra("validation_error") ?: "Sticker pack not added"
            _uiState.update { it.copy(stickerState = StickerState.Error(errorMessage)) }
        }
    }

    fun dismissStickerSheet() {
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

    fun saveToMyStickers() {
        val state = _uiState.value
        if (state.saveToMyStickersState is SaveToMyStickersState.Loading) return
        _uiState.update { it.copy(saveToMyStickersState = SaveToMyStickersState.Loading) }
        viewModelScope.launch {
            val result = if (state.autoCutState is AutoCutState.Ready) {
                myStickersRepository.saveFromUri(state.autoCutState.localUri)
            } else {
                myStickersRepository.saveFromUrl(state.imageUrl, state.mediaId)
            }
            result
                .onSuccess { _uiState.update { it.copy(saveToMyStickersState = SaveToMyStickersState.Success) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(saveToMyStickersState = SaveToMyStickersState.Error(e.message ?: "Failed to save"))
                    }
                }
        }
    }

    fun dismissSaveToMyStickers() {
        _uiState.update { it.copy(saveToMyStickersState = SaveToMyStickersState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        (_uiState.value.autoCutState as? AutoCutState.Ready)?.let { ready ->
            deleteTempFile(ready.localUri)
        }
    }

    private fun deleteTempFile(localUri: String) {
        File(localUri.removePrefix("file://")).delete()
    }
}
