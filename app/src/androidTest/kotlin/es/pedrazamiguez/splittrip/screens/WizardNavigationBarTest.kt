package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardFastTrackBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke and interaction tests for [WizardNavigationBar] and [WizardFastTrackBanner].
 */
@RunWith(AndroidJUnit4::class)
class WizardNavigationBarTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun rendersBackAndNextButtons_andInvokesCallbacks() {
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
                        submitLabel = "Create"
                    ),
                    onBack = { backClicked = true },
                    onNext = { nextClicked = true },
                    onSubmit = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Next").performClick()
        composeRule.waitForIdle()

        assertTrue(backClicked)
        assertTrue(nextClicked)
    }

    @Test
    fun rendersSubmitButton_whenOnLastStep() {
        var submitted = false

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
                        submitLabel = "Create Expense"
                    ),
                    onBack = {},
                    onNext = {},
                    onSubmit = { submitted = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Create Expense").assertIsDisplayed()
        composeRule.onNodeWithText("Back").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertDoesNotExist()

        composeRule.onNodeWithText("Create Expense").performClick()
        composeRule.waitForIdle()

        assertTrue(submitted)
    }

    @Test
    fun rendersFastTrackBannerAndHint_andInvokesCallback() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardFastTrackBanner(
                    label = "Skip to Review",
                    optionalDefaultsHint = "Defaults: equal split · paid today",
                    onSkipToReview = { skipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Skip to Review").assertIsDisplayed()
        composeRule.onNodeWithText("Defaults: equal split · paid today").assertIsDisplayed()

        composeRule.onNodeWithText("Skip to Review").performClick()
        composeRule.waitForIdle()

        assertTrue(skipped)
    }

    @Test
    fun rendersFastTrackBannerWithoutHint_whenOptionalDefaultsHintIsNull() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardFastTrackBanner(
                    label = "Skip to Review",
                    optionalDefaultsHint = null,
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
    fun disablesFastTrackBanner_whenNotEnabled() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme {
                WizardFastTrackBanner(
                    label = "Skip to Review",
                    optionalDefaultsHint = "Defaults: equal split · paid today",
                    enabled = false,
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
    fun rendersFastTrackBannerInDarkTheme_preservingActions() {
        var skipped = false

        composeRule.setContent {
            SplitTripTheme(darkTheme = true) {
                Column {
                    WizardFastTrackBanner(
                        label = "Skip to Review",
                        optionalDefaultsHint = "Defaults: equal split · paid today",
                        onSkipToReview = { skipped = true }
                    )
                    WizardNavigationBar(
                        config = WizardNavigationBarConfig(
                            canGoNext = true,
                            isOnLastStep = false,
                            isCurrentStepValid = true,
                            isLoading = false,
                            backLabel = "Back",
                            nextLabel = "Next",
                            submitLabel = "Create"
                        ),
                        onBack = {},
                        onNext = {},
                        onSubmit = {}
                    )
                }
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
