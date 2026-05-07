package com.gustavo.brilhante.cutecats.feature.dogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gustavo.brilhante.cutecats.core.common.network.DogsApi
import com.gustavo.brilhante.cutecats.core.domain.MediaRepository
import com.gustavo.brilhante.cutecats.core.ui.DiscoverUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DogsViewModel @Inject constructor(
    @DogsApi private val mediaRepository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<DiscoverUiState> = mediaRepository.getMedia()
        .map<_, DiscoverUiState> { DiscoverUiState.Success(it) }
        .catch { emit(DiscoverUiState.Error(it.message ?: "Unknown Error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiscoverUiState.Loading
        )
}
