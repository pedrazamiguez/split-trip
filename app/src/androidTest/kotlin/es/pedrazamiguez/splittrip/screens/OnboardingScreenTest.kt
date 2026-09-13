package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.onboarding.R
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.OnboardingScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [OnboardingScreen].
 *
 * Verifies that the tutorial stepper renders steps, controls, and triggers callbacks.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersOnboardingScreen_firstStep_showsNextAndSkipButtons() {
        val firstStepTitle = context.getString(R.string.onboarding_step_trips_title)
        val nextButtonText = context.getString(R.string.onboarding_next_button)
        val skipButtonText = context.getString(R.string.onboarding_skip_button)

        composeRule.setContent {
            SplitTripTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(
                        currentStep = OnboardingStep.TRIPS_AND_GROUPS
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(firstStepTitle).assertIsDisplayed()
        composeRule.onNodeWithText(nextButtonText).assertIsDisplayed()
        composeRule.onNodeWithText(skipButtonText).assertIsDisplayed()
    }

    @Test
    fun rendersOnboardingScreen_lastStep_showsCompleteButton() {
        val completeButtonText = context.getString(R.string.onboarding_complete_button)
        val lastStepTitle = context.getString(R.string.onboarding_step_settle_title)

        composeRule.setContent {
            SplitTripTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(
                        currentStep = OnboardingStep.CONSENSUS_AND_SETTLEMENT
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(lastStepTitle).assertIsDisplayed()
        composeRule.onNodeWithText(completeButtonText).assertIsDisplayed()
    }

    @Test
    fun completeButton_isClickable() {
        val completeButtonText = context.getString(R.string.onboarding_complete_button)
        var wasCompleted = false

        composeRule.setContent {
            SplitTripTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(
                        currentStep = OnboardingStep.CONSENSUS_AND_SETTLEMENT
                    ),
                    onCompleteClick = { wasCompleted = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(completeButtonText).assertIsDisplayed()
        composeRule.onNodeWithText(completeButtonText).performClick()
        composeRule.waitForIdle()

        assertTrue("Expected onCompleteClick callback to fire", wasCompleted)
    }

    @Test
    fun skipButton_isClickable() {
        val skipButtonText = context.getString(R.string.onboarding_skip_button)
        var wasSkipped = false

        composeRule.setContent {
            SplitTripTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(
                        currentStep = OnboardingStep.TRIPS_AND_GROUPS
                    ),
                    onSkipClick = { wasSkipped = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(skipButtonText).assertIsDisplayed()
        composeRule.onNodeWithText(skipButtonText).performClick()
        composeRule.waitForIdle()

        assertTrue("Expected onSkipClick callback to fire", wasSkipped)
    }
}
