package com.gustavo.brilhante.cutestickers.mediadetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class MediaDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mediaDetailsScreen_displaysHeroImageAndActionButtons() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(
                                imageUrl = "https://example.com/media.jpg",
                                mediaId = "123"
                            ),
                            snackbarHostState = remember { SnackbarHostState() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            onBackClick = {},
                            onAddToWhatsApp = {},
                            onDownload = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("hero_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("create_sticker_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("download_button").assertIsDisplayed()
    }

    @Test
    fun mediaDetailsScreen_stickerButtonEnabled_whenIdle() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(stickerState = StickerState.Idle),
                            snackbarHostState = remember { SnackbarHostState() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            onBackClick = {},
                            onAddToWhatsApp = {},
                            onDownload = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("create_sticker_button").assertIsEnabled()
    }

    @Test
    fun mediaDetailsScreen_downloadButtonEnabled_whenIdle() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(downloadState = DownloadState.Idle),
                            snackbarHostState = remember { SnackbarHostState() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this,
                            onBackClick = {},
                            onAddToWhatsApp = {},
                            onDownload = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("download_button").assertIsEnabled()
    }
}
