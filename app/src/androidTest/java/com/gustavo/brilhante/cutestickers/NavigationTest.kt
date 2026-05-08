package com.gustavo.brilhante.cutestickers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigateToDetailsAndCheckStickerButton() {
        // Wait for cats to load and click the first one
        // Note: In a real app we might need to handle idling resources for network
        
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag("media_card").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onAllNodesWithTag("media_card").onFirst().performClick()

        // Check if Media Details screen is shown
        // We wait for it because of navigation transitions
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("media_details_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("media_details_title").assertIsDisplayed()
        
        // Check if hero image is displayed
        composeTestRule.onNodeWithTag("hero_image").assertIsDisplayed()

        // Check if sticker button is displayed
        composeTestRule.onNodeWithTag("create_sticker_button").assertIsDisplayed()
    }
}
