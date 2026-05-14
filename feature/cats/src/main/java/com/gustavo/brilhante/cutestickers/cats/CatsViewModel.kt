package com.gustavo.brilhante.cutestickers.cats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutestickers.common.network.CatsApi
import com.gustavo.brilhante.cutestickers.domain.usecase.GetCachedMediaUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.LoadNextPageUseCase
import com.gustavo.brilhante.cutestickers.domain.usecase.RefreshMediaUseCase
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class CatsViewModel @Inject constructor(
    @CatsApi private val getCachedMediaUseCase: GetCachedMediaUseCase,
    @CatsApi private val refreshMediaUseCase: RefreshMediaUseCase,
    @CatsApi private val loadNextPageUseCase: LoadNextPageUseCase
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)
    private val errorState = MutableStateFlow<Pair<String, Boolean>?>(null)

    init {
        refresh()
    }

    val uiState: StateFlow<DiscoverUiState> = combine(
        getCachedMediaUseCase(),
        isRefreshing,
        isLoadingMore,
        errorState
    ) { items, refreshing, loadingMore, error ->
        when {
            error != null && items.isEmpty() -> {
                DiscoverUiState.Error(
                    message = error.first,
                    isNoInternet = error.second,
                    isRefreshing = refreshing
                )
            }
            items.isEmpty() -> {
                DiscoverUiState.Loading
            }
            else -> {
                DiscoverUiState.Success(
                    items = items,
                    isRefreshing = refreshing,
                    isLoadingMore = loadingMore
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiscoverUiState.Loading
    )

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            errorState.value = null
            try {
                refreshMediaUseCase()
            } catch (e: Exception) {
                errorState.value = (e.message ?: "Failed to refresh") to (e is UnknownHostException)
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
