package es.pedrazamiguez.splittrip.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.FundingSourceStep
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke and interaction tests for [FundingSourceStep].
 */
@RunWith(AndroidJUnit4::class)
class FundingSourceStepTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val groupSource = FundingSourceUiModel(
        id = PayerType.GROUP.name,
        displayText = "Group Pocket",
        description = "Shared group funds collected from contributions.",
        icon = TablerIcons.Outline.UsersGroup
    )

    private val userSource = FundingSourceUiModel(
        id = PayerType.USER.name,
        displayText = "Your Money",
        description = "Paid with your own money and tracked for reimbursement.",
        icon = TablerIcons.Outline.User
    )

    private val fundingSources = persistentListOf(groupSource, userSource)

    @Test
    fun rendersHeaderAndBothFundingSourceOptions_withDescriptions() {
        val title = context.getString(R.string.funding_source_title)
        val subtitle = context.getString(R.string.funding_source_subtitle)

        composeRule.setContent {
            SplitTripTheme {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = groupSource
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(subtitle).assertIsDisplayed()
        composeRule.onNodeWithText(groupSource.displayText).assertIsDisplayed()
        composeRule.onNodeWithText(groupSource.description).assertIsDisplayed()
        composeRule.onNodeWithText(userSource.displayText).assertIsDisplayed()
        composeRule.onNodeWithText(userSource.description).assertIsDisplayed()
    }

    @Test
    fun marksGroupPocketSelected_andYourMoneyUnselected_whenGroupSelected() {
        composeRule.setContent {
            SplitTripTheme {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = groupSource
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(groupSource.displayText).assertIsSelected()
        composeRule.onNodeWithText(userSource.displayText).assertIsNotSelected()
    }

    @Test
    fun marksYourMoneySelected_andGroupPocketUnselected_whenUserSelected() {
        composeRule.setContent {
            SplitTripTheme {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = userSource
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(userSource.displayText).assertIsSelected()
        composeRule.onNodeWithText(groupSource.displayText).assertIsNotSelected()
    }

    @Test
    fun tappingYourMoney_emitsFundingSourceSelectedAndAutoAdvances() {
        val capturedEvents = mutableListOf<AddExpenseUiEvent>()
        var autoAdvanced = false

        composeRule.setContent {
            SplitTripTheme {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = groupSource
                    ),
                    onEvent = { capturedEvents.add(it) },
                    onAutoAdvance = { autoAdvanced = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(userSource.displayText).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(AddExpenseUiEvent.FundingSourceSelected(PayerType.USER.name)),
            capturedEvents
        )
        assertTrue(autoAdvanced)
    }

    @Test
    fun tappingGroupPocket_emitsFundingSourceSelectedAndAutoAdvances() {
        val capturedEvents = mutableListOf<AddExpenseUiEvent>()
        var autoAdvanced = false

        composeRule.setContent {
            SplitTripTheme {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = userSource
                    ),
                    onEvent = { capturedEvents.add(it) },
                    onAutoAdvance = { autoAdvanced = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(groupSource.displayText).performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(AddExpenseUiEvent.FundingSourceSelected(PayerType.GROUP.name)),
            capturedEvents
        )
        assertTrue(autoAdvanced)
    }

    @Test
    fun rendersBothOptionsInDarkTheme_preservingSelectionSemantics() {
        val title = context.getString(R.string.funding_source_title)
        val subtitle = context.getString(R.string.funding_source_subtitle)

        composeRule.setContent {
            SplitTripTheme(darkTheme = true) {
                FundingSourceStep(
                    uiState = AddExpenseUiState(
                        fundingSources = fundingSources,
                        selectedFundingSource = userSource
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(subtitle).assertIsDisplayed()
        composeRule.onNodeWithText(groupSource.displayText).assertIsDisplayed()
        composeRule.onNodeWithText(groupSource.description).assertIsDisplayed()
        composeRule.onNodeWithText(userSource.displayText).assertIsDisplayed()
        composeRule.onNodeWithText(userSource.description).assertIsDisplayed()
        composeRule.onNodeWithText(userSource.displayText).assertIsSelected()
        composeRule.onNodeWithText(groupSource.displayText).assertIsNotSelected()
    }
}
