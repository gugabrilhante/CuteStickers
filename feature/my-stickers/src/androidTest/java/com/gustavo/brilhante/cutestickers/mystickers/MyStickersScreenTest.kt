package com.gustavo.brilhante.cutestickers.mystickers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gustavo.brilhante.cutestickers.designsystem.theme.CuteStickersTheme
import com.gustavo.brilhante.cutestickers.model.MediaItem
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalSharedTransitionApi::class)
class MyStickersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsTitleAndImportButton() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Empty,
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("empty_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_fab").assertDoesNotExist()
    }

    @Test
    fun emptyState_importButtonTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Empty,
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = { clicked = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("import_button").performClick()
        assert(clicked)
    }

    @Test
    fun successState_showsGrid() {
        val items = listOf(
            MediaItem(id = "1", url = "file:///path/img1.jpg"),
            MediaItem(id = "2", url = "file:///path/img2.jpg")
        )
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("my_stickers_grid").assertIsDisplayed()
    }

    @Test
    fun successState_importingShowsProgress() {
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = emptyList(), isImporting = true),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("import_progress").assertIsDisplayed()
    }

    // Feature 1: FAB replaces TopAppBar + button
    @Test
    fun successState_showsFab() {
        val items = listOf(MediaItem(id = "1", url = "file:///path/img1.jpg"))
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("import_fab").assertIsDisplayed()
    }

    @Test
    fun successState_showsFabWithText() {
        val items = listOf(MediaItem(id = "1", url = "file:///path/img1.jpg"))
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("import_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()
    }

    @Test
    fun successState_fabTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = emptyList()),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = { clicked = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("import_fab").performClick()
        assert(clicked)
    }

    // Feature 3: multi-select + delete UI
    @Test
    fun selectionMode_showsDeleteButton_andImportFab() {
        val items = listOf(MediaItem(id = "1", url = "file:///path/img1.jpg"))
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items, selectedIds = setOf("1")),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("delete_selected_fab").assertIsDisplayed()
        composeTestRule.onNodeWithTag("import_fab").assertIsDisplayed()
    }

    @Test
    fun selectionMode_deleteButtonTriggersCallback() {
        var deleted = false
        val items = listOf(MediaItem(id = "1", url = "file:///path/img1.jpg"))
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items, selectedIds = setOf("1")),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            onDeleteSelected = { deleted = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("delete_selected_fab").performClick()
        assert(deleted)
    }

    @Test
    fun selectionMode_clearSelectionButtonTriggersCallback() {
        var cleared = false
        val items = listOf(MediaItem(id = "1", url = "file:///path/img1.jpg"))
        composeTestRule.setContent {
            CuteStickersTheme {
                SharedTransitionLayout {
                    AnimatedVisibility(visible = true) {
                        MyStickersScreen(
                            uiState = MyStickersUiState.Success(items = items, selectedIds = setOf("1")),
                            snackbarHostState = remember { SnackbarHostState() },
                            onItemClick = {},
                            onImportClick = {},
                            onClearSelection = { cleared = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("clear_selection_button").performClick()
        assert(cleared)
    }
}
