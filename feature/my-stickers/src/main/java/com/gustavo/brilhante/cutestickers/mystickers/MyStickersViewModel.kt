package com.gustavo.brilhante.cutestickers.mystickers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.mystickers.domain.MySticker
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyStickersUiState {
    data object Loading : MyStickersUiState
    data object Empty : MyStickersUiState
    data class Success(
        val items: List<MediaItem>,
        val isImporting: Boolean = false,
        val importError: String? = null,
        val selectedIds: Set<String> = emptySet()
    ) : MyStickersUiState
}

@HiltViewModel
class MyStickersViewModel @Inject constructor(
    private val repository: MyStickersRepository
) : ViewModel() {

    private val isImporting = MutableStateFlow(false)
    private val importError = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<MyStickersUiState> = combine(
        repository.getStickers(),
        isImporting,
        importError,
        _selectedIds
    ) { stickers, importing, error, selectedIds ->
        if (stickers.isEmpty() && !importing && error == null) {
            MyStickersUiState.Empty
        } else {
            MyStickersUiState.Success(
                items = stickers.map { it.toMediaItem() },
                isImporting = importing,
                importError = error,
                selectedIds = selectedIds
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyStickersUiState.Loading
    )

    fun importFromGallery(uriString: String) {
        if (isImporting.value) return
        viewModelScope.launch {
            isImporting.value = true
            importError.value = null
            repository.saveFromUri(uriString)
                .onFailure { e -> importError.value = e.message ?: "Failed to import image" }
            isImporting.value = false
        }
    }

    fun clearImportError() {
        importError.value = null
    }

    fun toggleSelection(id: String) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toSet()
        if (ids.isEmpty()) return
        _selectedIds.value = emptySet()
        viewModelScope.launch {
            ids.forEach { id -> repository.deleteSticker(id) }
        }
    }
}

private fun MySticker.toMediaItem() = MediaItem(id = id, url = "file://$localPath")
