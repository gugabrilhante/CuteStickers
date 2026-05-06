package com.gustavo.brilhante.cutecats.feature.dogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gustavo.brilhante.cutecats.core.ui.AnimalGifScreen

@Composable
fun DogsRoute(
    viewModel: DogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimalGifScreen(
        title = "Cute Dogs",
        uiState = uiState
    )
}
