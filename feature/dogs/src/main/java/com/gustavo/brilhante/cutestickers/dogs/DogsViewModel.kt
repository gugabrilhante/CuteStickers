package com.gustavo.brilhante.cutestickers.dogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.common.network.NetworkMonitor
import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.mystickers.domain.MyStickersRepository
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiEvent
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class DogsViewModel @Inject constructor(
    @DogsApi private val getCachedMediaUseCase: GetCachedMediaUseCase,
    @DogsApi private val refreshMediaUseCase: RefreshMediaUseCase,
    @DogsApi private val loadNextPageUseCase: LoadNextPageUseCase,
    private val myStickersRepository: MyStickersRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)
    private val errorState = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    init {
        onEvent(DiscoverUiEvent.Refresh)
        viewModelScope.launch {
            networkMonitor.isOnline.drop(1).collect { isOnline ->
                if (isOnline) onEvent(DiscoverUiEvent.Refresh)
            }
        }
    }

    val uiState: StateFlow<DiscoverUiState> = combine(
        getCachedMediaUseCase(),
        isRefreshing,
        isLoadingMore,
        errorState,
        networkMonitor.isOnline
    ) { items, refreshing, loadingMore, error, isOnline ->
        when {
            items.isNotEmpty() -> {
                DiscoverUiState.Success(
                    items = items,
                    isRefreshing = refreshing,
                    isLoadingMore = loadingMore,
                    isOffline = !isOnline
                )
            }
            error != null -> {
                DiscoverUiState.Error(
                    message = error.first,
                    isNoInternet = error.second,
                    isRefreshing = refreshing
                )
            }
            else -> {
                DiscoverUiState.Loading
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState.Loading
    )

    fun onEvent(event: DiscoverUiEvent) {
        when (event) {
            is DiscoverUiEvent.Refresh -> refresh(force = true)
            is DiscoverUiEvent.LoadMore -> loadMore()
            is DiscoverUiEvent.OnItemClick -> { /* Handled in Screen */ }
            is DiscoverUiEvent.OnItemLongClick -> toggleSelection(event.item)
            is DiscoverUiEvent.ClearSelection -> clearSelection()
            is DiscoverUiEvent.SaveSelection -> saveSelectionToMyStickers()
            is DiscoverUiEvent.AboutClick -> { /* Handled in Screen */ }
        }
    }

    private fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            val items = getCachedMediaUseCase().first()
            if (force || items.isEmpty()) isRefreshing.value = true
            errorState.value = null
            clearSelection()
            try {
                refreshMediaUseCase(force)
            } catch (e: Exception) {
                errorState.value = (e.message ?: "Failed to refresh") to (e is UnknownHostException)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun loadMore() {
        viewModelScope.launch {
            if (isLoadingMore.value) return@launch
            isLoadingMore.value = true
            try {
                loadNextPageUseCase()
            } catch (e: Exception) {
                // Error handled
            } finally {
                isLoadingMore.value = false
            }
        }
    }

    private fun toggleSelection(item: MediaItem) {
        _selectedIds.update { current ->
            if (item.id in current) current - item.id else current + item.id
        }
    }

    private fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    private fun saveSelectionToMyStickers() {
        val selectedCopy = _selectedIds.value.toSet()
        if (selectedCopy.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            getCachedMediaUseCase().first()
                .filter { it.id in selectedCopy }
                .forEach { item -> myStickersRepository.saveFromUrl(item.url, item.id) }
        }
    }
}
