package com.gustavo.brilhante.cutecats.feature.cats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gustavo.brilhante.cutecats.core.ui.AnimalGifScreen

@Composable
fun CatsRoute(
    viewModel: CatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimalGifScreen(
        title = "Cute Cats",
        uiState = uiState
    )
}
