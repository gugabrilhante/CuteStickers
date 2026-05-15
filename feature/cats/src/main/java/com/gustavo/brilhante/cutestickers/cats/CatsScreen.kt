package com.gustavo.brilhante.cutestickers.cats

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.gustavo.brilhante.cutestickers.common.PreferencesManager
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.ui.DiscoverScreen
import com.gustavo.brilhante.cutestickers.ui.DiscoverUiEvent
import kotlinx.coroutines.launch
import com.gustavo.brilhante.cutestickers.ui.R as UiR

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatsRoute(
    onItemClick: (MediaItem) -> Unit,
    onAboutClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier,
    viewModel: CatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val onboardingShown by preferencesManager.isOnboardingShown.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    DiscoverScreen(
        uiState = uiState,
        selectedIds = selectedIds,
        onEvent = { event ->
            when (event) {
                is DiscoverUiEvent.OnItemClick -> {
                    if (selectedIds.isNotEmpty()) {
                        viewModel.onEvent(DiscoverUiEvent.OnItemLongClick(event.item))
                    } else {
                        onItemClick(event.item)
                    }
                }
                is DiscoverUiEvent.AboutClick -> onAboutClick()
                else -> viewModel.onEvent(event)
            }
        },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        badgeText = stringResource(id = UiR.string.tap_to_create_sticker),
        onboardingMessage = stringResource(id = UiR.string.onboarding_message),
        okText = stringResource(id = UiR.string.ok),
        showOnboarding = !onboardingShown,
        onOnboardingDismissed = {
            scope.launch {
                preferencesManager.setOnboardingShown(true)
            }
        },
        title = stringResource(id = UiR.string.cats),
        offlinePlaceholderRes = UiR.drawable.ic_offline_cat,
        modifier = modifier
    )
}
