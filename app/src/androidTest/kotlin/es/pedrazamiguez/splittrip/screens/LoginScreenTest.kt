package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.LoginScreen
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [LoginScreen].
 *
 * Verifies the stateless Screen composable renders correctly with
 * different [AuthenticationUiState] configurations. No ViewModel or
 * Koin dependencies needed — pure data in, UI out.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // ═════════════════════════════════════════════════════════════════════
    //  Default idle state
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun rendersLoginForm_inDefaultState() {
        val emailLabel = context.getString(R.string.login_email_label)
        val passwordLabel = context.getString(R.string.login_password_label)
        val googleButton = context.getString(R.string.login_google_button)
        val emailDisclosureButton = context.getString(R.string.login_continue_with_email)
        val guestButton = context.getString(R.string.login_guest_button)
        val startJourney = context.getString(R.string.login_start_journey)

        composeRule.setContent {
            SplitTripTheme {
                LoginScreen(uiState = AuthenticationUiState())
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(googleButton).assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText(emailDisclosureButton).assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText(guestButton).assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText(startJourney).assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText(emailLabel).assertDoesNotExist()
        composeRule.onNodeWithText(passwordLabel).assertDoesNotExist()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Email form expanded state
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun rendersLoginForm_whenEmailExpanded() {
        val emailLabel = context.getString(R.string.login_email_label)
        val passwordLabel = context.getString(R.string.login_password_label)
        val loginButton = context.getString(R.string.login_button)

        composeRule.setContent {
            SplitTripTheme {
                LoginScreen(
                    uiState = AuthenticationUiState(isEmailFormExpanded = true)
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emailLabel).assertIsDisplayed()
        composeRule.onNodeWithText(passwordLabel).assertIsDisplayed()
        composeRule.onNodeWithText(loginButton).assertIsDisplayed().assertIsEnabled()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Loading state disables controls
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun disablesControls_whenLoading() {
        val emailLabel = context.getString(R.string.login_email_label)
        val passwordLabel = context.getString(R.string.login_password_label)

        composeRule.setContent {
            SplitTripTheme {
                LoginScreen(
                    uiState = AuthenticationUiState(
                        isEmailFormExpanded = true,
                        isLoading = true
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(emailLabel).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(passwordLabel).assertIsNotEnabled()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Error state shows error message
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun showsErrorMessage_whenErrorIsPresent() {
        val errorMessage = "Test error message"

        composeRule.setContent {
            SplitTripTheme {
                LoginScreen(
                    uiState = AuthenticationUiState(
                        error = UiText.DynamicString(errorMessage)
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Google sign-in hidden when unavailable
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun hidesGoogleSignIn_whenNotAvailable() {
        val googleButton = context.getString(R.string.login_google_button)
        val emailDisclosureButton = context.getString(R.string.login_continue_with_email)
        val emailLabel = context.getString(R.string.login_email_label)
        val passwordLabel = context.getString(R.string.login_password_label)

        composeRule.setContent {
            SplitTripTheme {
                LoginScreen(
                    uiState = AuthenticationUiState(),
                    isGoogleSignInAvailable = false
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(googleButton).assertDoesNotExist()
        composeRule.onNodeWithText(emailDisclosureButton).assertDoesNotExist()
        composeRule.onNodeWithText(emailLabel).assertIsDisplayed()
        composeRule.onNodeWithText(passwordLabel).assertIsDisplayed()
    }
}
