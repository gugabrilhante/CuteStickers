package com.gustavo.brilhante.cutestickers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutestickers.model.MediaItem
import com.gustavo.brilhante.cutestickers.model.MediaType
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class DiscoverScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun discoverScreen_displaysGifBadge_forAnimatedItems() {
        val items = listOf(
            MediaItem(id = "1", url = ""),
            MediaItem(id = "2", url = "", type = MediaType.Animated)
        )
        val uiState = DiscoverUiState.Success(items = items)

        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverScreen(
                            uiState = uiState,
                            onItemClick = {},
                            onRefresh = {},
                            onLoadMore = {},
                            onAboutClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            badgeText = "Tap to create sticker",
                            onboardingMessage = "Welcome",
                            okText = "OK",
                            showOnboarding = false,
                            onOnboardingDismissed = {},
                            title = "Discover"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("GIF").assertIsDisplayed()
    }

    @Test
    fun discoverScreen_hidesSaveFab_whenRefreshing() {
        val items = listOf(MediaItem(id = "1", url = ""))
        val uiState = DiscoverUiState.Success(items = items, isRefreshing = true)

        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverScreen(
                            uiState = uiState,
                            onItemClick = {},
                            onRefresh = {},
                            onLoadMore = {},
                            onAboutClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            badgeText = "Tap",
                            onboardingMessage = "",
                            okText = "",
                            showOnboarding = false,
                            onOnboardingDismissed = {},
                            title = "Discover",
                            selectedIds = setOf("1")
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("save_selection_fab").assertDoesNotExist()
    }

    @Test
    fun discoverScreen_showsOfflineBanner_whenIsOfflineTrue() {
        val items = listOf(MediaItem(id = "1", url = ""))
        val uiState = DiscoverUiState.Success(items = items, isOffline = true)

        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverScreen(
                            uiState = uiState,
                            onItemClick = {},
                            onRefresh = {},
                            onLoadMore = {},
                            onAboutClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            badgeText = "Tap",
                            onboardingMessage = "",
                            okText = "",
                            showOnboarding = false,
                            onOnboardingDismissed = {},
                            title = "Cats"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("offline_banner").assertIsDisplayed()
    }

    @Test
    fun discoverScreen_hidesOfflineBanner_whenIsOfflineFalse() {
        val items = listOf(MediaItem(id = "1", url = ""))
        val uiState = DiscoverUiState.Success(items = items, isOffline = false)

        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverScreen(
                            uiState = uiState,
                            onItemClick = {},
                            onRefresh = {},
                            onLoadMore = {},
                            onAboutClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            badgeText = "Tap",
                            onboardingMessage = "",
                            okText = "",
                            showOnboarding = false,
                            onOnboardingDismissed = {},
                            title = "Cats"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("offline_banner").assertDoesNotExist()
    }

    @Test
    fun discoverScreen_swipeRefreshOffline_doesNotLeaveInfiniteSpinner() {
        val items = listOf(MediaItem(id = "1", url = ""))
        var uiState: DiscoverUiState = DiscoverUiState.Success(items = items, isOffline = true)

        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        DiscoverScreen(
                            uiState = uiState,
                            onItemClick = {},
                            onRefresh = { uiState = DiscoverUiState.Success(items = items, isRefreshing = false, isOffline = true) },
                            onLoadMore = {},
                            onAboutClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            badgeText = "Tap",
                            onboardingMessage = "",
                            okText = "",
                            showOnboarding = false,
                            onOnboardingDismissed = {},
                            title = "Cats"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("offline_banner").assertIsDisplayed()
    }
}
