package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.ProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ProfileScreen].
 *
 * Verifies the stateless Screen composable renders correctly with
 * different [ProfileUiState] configurations — loading, loaded, and
 * error states. No ViewModel or Koin dependencies needed.
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    // ═════════════════════════════════════════════════════════════════════
    //  Profile loaded — displays user info
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun rendersProfileInfo_whenProfileIsLoaded() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = ProfileUiModel(
                            displayName = "Jane Doe",
                            email = "jane@example.com",
                            profileImageUrl = null,
                            bio = "This is Jane Doe's bio."
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeRule.onNodeWithText("jane@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("This is Jane Doe's bio.", substring = true).assertIsDisplayed()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Loading state — does not crash
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun rendersWithoutCrash_whenLoading() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(isLoading = true)
                )
            }
        }

        composeRule.waitForIdle()

        // No crash = success. Profile info should not be visible.
        composeRule.onNodeWithText("Jane Doe").assertDoesNotExist()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Error state — shows retry button (error message delivered via Snackbar)
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsRetryButton_whenErrorIsPresent() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        hasError = true
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Retry", substring = true).assertIsDisplayed()
    }
}
