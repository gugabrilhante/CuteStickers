package com.gustavo.brilhante.cutecats.feature.cats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutecats.core.common.network.CatsApi
import com.gustavo.brilhante.cutecats.core.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutecats.core.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutecats.core.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutecats.core.ui.DiscoverUiState
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
class CatsViewModel @Inject constructor(
    @CatsApi private val getCachedMediaUseCase: GetCachedMediaUseCase,
    @CatsApi private val refreshMediaUseCase: RefreshMediaUseCase,
    @CatsApi private val loadNextPageUseCase: LoadNextPageUseCase
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
                // Error handled by the flow or could be emitted as a side effect
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
