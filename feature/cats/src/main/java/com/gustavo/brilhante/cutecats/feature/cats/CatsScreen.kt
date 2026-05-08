package com.gustavo.brilhante.cutecats.feature.cats

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
fun CatsRoute(
    onItemClick: (MediaItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    viewModel: CatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        title = "Cats",
        uiState = uiState,
        onItemClick = onItemClick,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = modifier
    )
}
