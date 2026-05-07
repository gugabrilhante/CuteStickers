package com.gustavo.brilhante.cutecats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import org.junit.Rule
import org.junit.Test

class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigateToDetailsAndCheckStickerButton() {
        // Wait for cats to load and click the first one
        // Note: In a real app we might need to handle idling resources for network
        
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("image-", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("image-", substring = true).onFirst().performClick()

        // Check if Media Details screen is shown
        composeTestRule.onNodeWithText("Media Details").assertIsDisplayed()
        
        // Check if hero image is displayed
        composeTestRule.onNodeWithTag("hero_image").assertIsDisplayed()

        // Check if sticker button is displayed
        composeTestRule.onNodeWithTag("create_sticker_button").assertIsDisplayed()
    }
}
