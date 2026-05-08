package com.gustavo.brilhante.cutecats.feature.dogs

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gustavo.brilhante.cutecats.core.model.MediaItem
import com.gustavo.brilhante.cutecats.core.ui.DiscoverScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DogsRoute(
    onItemClick: (MediaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: DogsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        title = "Dogs",
        uiState = uiState,
        onItemClick = onItemClick,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = modifier
    )
}
