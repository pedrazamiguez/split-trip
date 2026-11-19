package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.DISPUTE_ATTACHMENT_CONTAINER_TEST_TAG
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.DISPUTE_REASON_INPUT_TEST_TAG
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.DisputeSettlementScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.DisputeSettlementUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.DisputeSettlementUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [DisputeSettlementScreen].
 */
@RunWith(AndroidJUnit4::class)
class DisputeSettlementScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersInputAndSubmitButton_andDisablesSubmitWhenReasonIsBlank() {
        val hintText = context.getString(R.string.your_balance_dispute_dialog_hint)
        val submitText = context.getString(R.string.your_balance_dispute_dialog_submit)

        composeRule.setContent {
            SplitTripTheme {
                DisputeSettlementScreen(
                    uiState = DisputeSettlementUiState(reason = ""),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DISPUTE_REASON_INPUT_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(hintText).assertIsDisplayed()
        composeRule.onNodeWithText(submitText).assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag(DISPUTE_ATTACHMENT_CONTAINER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun enablesSubmit_whenReasonIsNotBlankAndOnline_andTriggersCallbacks() {
        val submitText = context.getString(R.string.your_balance_dispute_dialog_submit)
        val events = mutableListOf<DisputeSettlementUiEvent>()
        var uiState by mutableStateOf(DisputeSettlementUiState(reason = ""))

        composeRule.setContent {
            SplitTripTheme {
                DisputeSettlementScreen(
                    uiState = uiState,
                    onEvent = { event ->
                        events.add(event)
                        if (event is DisputeSettlementUiEvent.ReasonChanged) {
                            uiState = uiState.copy(reason = event.reason)
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(
            hasSetTextAction() and hasAnyAncestor(hasTestTag(DISPUTE_REASON_INPUT_TEST_TAG))
        ).performTextInput("Wrong amount")

        composeRule.waitForIdle()

        assertTrue(events.contains(DisputeSettlementUiEvent.ReasonChanged("Wrong amount")))
        composeRule.onNodeWithText(submitText).assertIsEnabled().performClick()

        composeRule.waitForIdle()

        assertEquals(
            listOf(
                DisputeSettlementUiEvent.ReasonChanged("Wrong amount"),
                DisputeSettlementUiEvent.SubmitClicked
            ),
            events
        )
    }

    @Test
    fun disablesSubmitAndShowsWarningBanner_whenOffline() {
        val offlineWarning = context.getString(R.string.your_balance_offline_warning)
        val submitText = context.getString(R.string.your_balance_dispute_dialog_submit)

        composeRule.setContent {
            SplitTripTheme {
                DisputeSettlementScreen(
                    uiState = DisputeSettlementUiState(
                        reason = "Wrong amount",
                        isOffline = true
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(offlineWarning).assertIsDisplayed()
        composeRule.onNodeWithText(submitText).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun rendersExtensibleAttachmentContainerContent_whenProvided() {
        composeRule.setContent {
            SplitTripTheme {
                DisputeSettlementScreen(
                    uiState = DisputeSettlementUiState(),
                    onEvent = {},
                    attachmentContent = {
                        Text(text = "Sticker Picker Slot")
                    }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(DISPUTE_ATTACHMENT_CONTAINER_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Sticker Picker Slot").assertIsDisplayed()
    }

    @Test
    fun featureScaffold_tabHostedMode_consumesBottomSystemInsets() {
        var outerBottomPx = -1
        var innerBottomPx = -1

        composeRule.setContent {
            SplitTripTheme {
                CompositionLocalProvider(
                    LocalTopPadding provides 56.dp,
                    LocalBottomPadding provides 0.dp
                ) {
                    FeatureScaffold(currentRoute = Routes.DISPUTE_SETTLEMENT) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .onGloballyPositioned { coordinates ->
                                    outerBottomPx = coordinates.boundsInRoot().bottom.toInt()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .onGloballyPositioned { coordinates ->
                                        innerBottomPx = coordinates.boundsInRoot().bottom.toInt()
                                    }
                            )
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        assertTrue(outerBottomPx > 0)
        assertEquals(0, outerBottomPx - innerBottomPx)
    }
}
