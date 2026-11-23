package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.PROFILE_AVATAR_PRO_BADGE_TEST_TAG
import es.pedrazamiguez.splittrip.features.profile.R as ProfileR
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

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

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
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun rendersProfileInfo_withProBadge_whenUserIsPro() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = ProfileUiModel(
                            displayName = "Jane Doe",
                            email = "jane@example.com",
                            profileImageUrl = null,
                            bio = "This is Jane Doe's bio.",
                            isPro = true
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_AVATAR_PRO_BADGE_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("PRO").assertIsDisplayed()
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

    // ═════════════════════════════════════════════════════════════════════
    //  Horizon Narrative — initials avatar, empty bio, QR share section
    // ═════════════════════════════════════════════════════════════════════

    @Test
    fun rendersAvatarInitials_whenProfileImageUrlIsNull() {
        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = ProfileUiModel(
                            displayName = "Jane Doe",
                            email = "jane@example.com",
                            profileImageUrl = null
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("JD").assertIsDisplayed()
    }

    @Test
    fun rendersEmptyBioPlaceholder_whenBioIsBlank() {
        val emptyBioText = context.getString(ProfileR.string.profile_bio_empty)

        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = ProfileUiModel(
                            displayName = "Jane Doe",
                            email = "jane@example.com",
                            bio = ""
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyBioText).assertIsDisplayed()
    }

    @Test
    fun rendersQrShareSection_whenProfileIsLoaded() {
        val qrTitleText = context.getString(ProfileR.string.profile_qr_title)

        composeRule.setContent {
            SplitTripTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        isLoading = false,
                        profile = ProfileUiModel(
                            userId = "u1",
                            displayName = "Jane Doe",
                            email = "jane@example.com"
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(qrTitleText).assertIsDisplayed()
    }
}
