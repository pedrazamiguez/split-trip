package es.pedrazamiguez.splittrip.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [EmptyStateView].
 */
@RunWith(AndroidJUnit4::class)
class EmptyStateViewTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun rendersEmptyState_withTitleAndDescription() {
        composeRule.setContent {
            SplitTripTheme {
                EmptyStateView(
                    title = "No items found",
                    description = "Try creating a new one"
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("No items found").assertIsDisplayed()
        composeRule.onNodeWithText("Try creating a new one").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyState_withActionButton_andHandlesClick() {
        var clicked = false

        composeRule.setContent {
            SplitTripTheme {
                EmptyStateView(
                    title = "Failed to load",
                    action = {
                        Button(onClick = { clicked = true }) {
                            Text("Retry")
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.waitForIdle()

        assertTrue(clicked)
    }
}
