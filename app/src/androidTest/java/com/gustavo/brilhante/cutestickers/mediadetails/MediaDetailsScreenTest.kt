package com.gustavo.brilhante.cutestickers.mediadetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.gustavo.brilhante.cutestickers.mediadetails.DownloadState
import com.gustavo.brilhante.cutestickers.mediadetails.MediaDetailsScreen
import com.gustavo.brilhante.cutestickers.mediadetails.MediaDetailsUiState
import com.gustavo.brilhante.cutestickers.mediadetails.StickerState
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutestickers.model.MediaType
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.gustavo.brilhante.cutestickers.ui.R as UiR
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@OptIn(ExperimentalSharedTransitionApi::class)
class MediaDetailsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
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

    @Test
    fun mediaDetailsScreen_displaysGifBadge_whenMediaIsAnimated() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(
                                imageUrl = "https://example.com/media.gif",
                                mediaId = "123",
                                mediaType = MediaType.Animated
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

        composeTestRule.onNodeWithText("GIF").assertIsDisplayed()
    }

    @Test
    fun mediaDetailsScreen_showsStickerPreview_onSuccess() {
        val pack = com.gustavo.brilhante.cutestickers.stickers.domain.StickerPack(
            id = "1",
            name = "Test Pack",
            publisher = "Tester",
            trayImageFileName = "tray.png",
            stickers = listOf(com.gustavo.brilhante.cutestickers.stickers.domain.StickerItem("s1.webp", emptyList())),
            isAnimated = true
        )
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(
                                stickerState = StickerState.Success(pack)
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

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val myStickersTitle = context.getString(UiR.string.my_stickers)

        composeTestRule.onNodeWithTag("sticker_preview_image").assertIsDisplayed()
        composeTestRule.onNodeWithText(myStickersTitle).assertIsDisplayed()
    }

    @Test
    fun mediaDetailsScreen_displaysErrorSnackbar_onDownloadFailure() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MediaDetailsScreen(
                            uiState = MediaDetailsUiState(
                                downloadState = DownloadState.Error("Network Error")
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

        composeTestRule.onNodeWithText("Network Error").assertIsDisplayed()
    }
}
