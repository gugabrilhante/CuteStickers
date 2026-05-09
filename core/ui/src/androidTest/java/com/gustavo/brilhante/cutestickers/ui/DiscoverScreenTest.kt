package com.gustavo.brilhante.cutestickers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
            MediaItem(id = "1", url = "https://example.com/image.jpg"),
            MediaItem(id = "2", url = "https://example.com/anim.gif", type = MediaType.Animated)
        )
        val uiState = DiscoverUiState.Success(items = items)

        composeTestRule.setContent {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    DiscoverScreen(
                        uiState = uiState,
                        onItemClick = {},
                        onRefresh = {},
                        onLoadMore = {},
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("GIF").assertIsDisplayed()
    }
}
