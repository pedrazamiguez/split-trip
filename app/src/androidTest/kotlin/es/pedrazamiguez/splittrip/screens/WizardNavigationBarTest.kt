package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke and interaction tests for [WizardNavigationBar].
 */
@RunWith(AndroidJUnit4::class)
class WizardNavigationBarTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun rendersBackAndNextButtons_whenFastTrackHidden() {
        var backClicked = false
        var nextClicked = false

        composeRule.setContent {
            SplitTripTheme {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = false,
                        isCurrentStepValid = true,
                        isLoading = false,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create",
                        skipToReviewLabel = null,
                        optionalDefaultsHint = null
                    ),
                    onBack = { backClicked = true },
                    onNext = { nextClicked = true },
                    onSubmit = {},
                    onSkipToReview = null
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()
        composeRule.onNodeWithText("Skip to Review").assertDoesNotExist()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()

        assertTrue(backClicked)
        assertTrue(nextClicked)
    }

    @Test
    fun rendersFastTrackPillAndHint_andInvokesCallback_whenVisible() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = false,
                        isCurrentStepValid = true,
                        isLoading = false,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create",
                        skipToReviewLabel = "Skip to Review",
                        optionalDefaultsHint = "Defaults: equal split · paid today"
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = {},
                    onSkipToReview = { skipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Skip to Review").assertIsDisplayed()
        composeRule.onNodeWithText("Defaults: equal split · paid today").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()

        composeRule.onNodeWithText("Skip to Review").performClick()
        composeRule.waitForIdle()

        assertTrue(skipped)
    }

    @Test
    fun rendersFastTrackWithoutHint_whenOptionalDefaultsHintIsNull() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = false,
                        isCurrentStepValid = true,
                        isLoading = false,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create",
                        skipToReviewLabel = "Skip to Review",
                        optionalDefaultsHint = null
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = {},
                    onSkipToReview = { skipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Skip to Review").assertIsDisplayed()
        composeRule.onNodeWithText("Skip to Review").performClick()
        composeRule.waitForIdle()

        assertTrue(skipped)
    }

    @Test
    fun hidesFastTrackPillAndHint_whenOnLastStep() {
        composeRule.setContent {
            SplitTripTheme {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = true,
                        isCurrentStepValid = true,
                        isLoading = false,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create Expense",
                        skipToReviewLabel = "Skip to Review",
                        optionalDefaultsHint = "Defaults: equal split · paid today"
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = {},
                    onSkipToReview = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Create Expense").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Skip to Review").assertDoesNotExist()
        composeRule.onNodeWithText("Defaults: equal split · paid today").assertDoesNotExist()
    }

    @Test
    fun disablesFastTrackPill_whenLoading() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = false,
                        isCurrentStepValid = true,
                        isLoading = true,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create",
                        skipToReviewLabel = "Skip to Review",
                        optionalDefaultsHint = "Defaults: equal split · paid today"
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = {},
                    onSkipToReview = { skipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Skip to Review").assertIsDisplayed()
        composeRule.onNodeWithText("Skip to Review").assertIsNotEnabled()
        composeRule.onNodeWithText("Skip to Review").performClick()
        composeRule.waitForIdle()

        assertFalse(skipped)
    }

    @Test
    fun rendersFastTrackInDarkTheme_preservingActions() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme(darkTheme = true) {
                WizardNavigationBar(
                    config = WizardNavigationBarConfig(
                        canGoNext = true,
                        isOnLastStep = false,
                        isCurrentStepValid = true,
                        isLoading = false,
                        backLabel = "Back",
                        nextLabel = "Next",
                        submitLabel = "Create",
                        skipToReviewLabel = "Skip to Review",
                        optionalDefaultsHint = "Defaults: equal split · paid today"
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = {},
                    onSkipToReview = { skipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Skip to Review").assertIsDisplayed()
        composeRule.onNodeWithText("Defaults: equal split · paid today").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()

        composeRule.onNodeWithText("Skip to Review").performClick()
        composeRule.waitForIdle()

        assertTrue(skipped)
    }
}
