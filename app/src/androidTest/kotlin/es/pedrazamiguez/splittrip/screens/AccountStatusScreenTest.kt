package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.AccountStatusScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [AccountStatusScreen].
 */
@RunWith(AndroidJUnit4::class)
class AccountStatusScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersAuthenticatedAccountStatus_showsVerifiedBadgeJoinDateProvidersAndDeleteSection() {
        val verifiedText = context.getString(R.string.account_security_status_verified)
        val emailProviderText = context.getString(R.string.account_status_provider_email_password)
        val googleProviderText = context.getString(R.string.account_status_provider_google)
        val deleteButtonText = context.getString(R.string.account_status_delete_account_button)

        composeRule.setContent {
            SplitTripTheme {
                AccountStatusScreen(
                    uiState = AccountStatusUiState(
                        isLoading = false,
                        email = "user@example.com",
                        isAnonymous = false,
                        joinDateText = "August 2026",
                        linkedProviders = persistentListOf(
                            AuthProviderType.EMAIL_PASSWORD,
                            AuthProviderType.GOOGLE
                        )
                    ),
                    onLinkGoogleClick = {},
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("user@example.com").assertIsDisplayed()
        composeRule.onNodeWithText(verifiedText).assertIsDisplayed()
        composeRule.onNodeWithText("Member since August 2026").assertIsDisplayed()
        composeRule.onNodeWithText(emailProviderText).assertIsDisplayed()
        composeRule.onNodeWithText(googleProviderText).assertIsDisplayed()
        composeRule.onNodeWithText(deleteButtonText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rendersUnlinkedGoogleProvider_clickingLinkInvokesCallback() {
        val notLinkedText = context.getString(R.string.account_status_not_linked)
        val linkButtonText = context.getString(R.string.account_status_link_button)
        var googleLinkClicked = false

        composeRule.setContent {
            SplitTripTheme {
                AccountStatusScreen(
                    uiState = AccountStatusUiState(
                        isLoading = false,
                        email = "user@example.com",
                        isAnonymous = false,
                        linkedProviders = persistentListOf(AuthProviderType.EMAIL_PASSWORD)
                    ),
                    onLinkGoogleClick = { googleLinkClicked = true },
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(notLinkedText).assertIsDisplayed()
        composeRule.onNodeWithText(linkButtonText).performClick()
        composeRule.waitForIdle()

        assertTrue(googleLinkClicked)
    }

    @Test
    fun rendersAnonymousAccountStatus_showsGuestWarningAndHidesDeleteButton() {
        val guestLabelText = context.getString(R.string.account_status_guest_label)
        val guestWarningText = context.getString(R.string.account_status_guest_warning)
        val deleteButtonText = context.getString(R.string.account_status_delete_account_button)

        composeRule.setContent {
            SplitTripTheme {
                AccountStatusScreen(
                    uiState = AccountStatusUiState(
                        isLoading = false,
                        isAnonymous = true,
                        linkedProviders = persistentListOf()
                    ),
                    onLinkGoogleClick = {},
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(guestLabelText).assertIsDisplayed()
        composeRule.onNodeWithText(guestWarningText).assertIsDisplayed()
        composeRule.onNodeWithText(deleteButtonText).assertDoesNotExist()
    }

    @Test
    fun clickingDeleteAccountButton_emitsShowDeleteAccountDialogEvent() {
        val deleteButtonText = context.getString(R.string.account_status_delete_account_button)
        val emittedEvents = mutableListOf<AccountStatusUiEvent>()

        composeRule.setContent {
            SplitTripTheme {
                AccountStatusScreen(
                    uiState = AccountStatusUiState(
                        isLoading = false,
                        email = "user@example.com",
                        isAnonymous = false,
                        linkedProviders = persistentListOf(AuthProviderType.EMAIL_PASSWORD)
                    ),
                    onLinkGoogleClick = {},
                    onEvent = { emittedEvents.add(it) }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(deleteButtonText).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(AccountStatusUiEvent.ShowDeleteAccountDialog), emittedEvents)
    }
}
