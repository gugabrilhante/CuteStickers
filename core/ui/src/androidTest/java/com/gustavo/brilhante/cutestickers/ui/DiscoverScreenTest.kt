package com.gustavo.brilhante.cutestickers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
