package com.gustavo.brilhante.cutestickers.dogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutestickers.common.network.DogsApi
import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DogsViewModel @Inject constructor(
    @DogsApi private val getCachedMediaUseCase: GetCachedMediaUseCase,
    @DogsApi private val refreshMediaUseCase: RefreshMediaUseCase,
    @DogsApi private val loadNextPageUseCase: LoadNextPageUseCase
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)

    val uiState: StateFlow<DiscoverUiState> = combine(
        getCachedMediaUseCase(),
        isRefreshing,
        isLoadingMore
    ) { items, refreshing, loadingMore ->
        if (items.isEmpty() && !refreshing && !loadingMore) {
            DiscoverUiState.Loading
        } else {
            DiscoverUiState.Success(
                items = items,
                isRefreshing = refreshing,
                isLoadingMore = loadingMore
            )
        }
    }.catch { emit(DiscoverUiState.Error(it.message ?: "Unknown Error")) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState.Loading
    )

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            try {
                refreshMediaUseCase()
            } catch (e: Exception) {
                // Error handled
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
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
}
