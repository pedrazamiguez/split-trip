package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpenseDetailScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ExpenseDetailScreen].
 */
@RunWith(AndroidJUnit4::class)
class ExpenseDetailScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    @Test
    fun rendersNotesVerbatimWithoutSyntheticQuotes_whenNotesPresent() {
        val notes = "Team dinner notes verbatim"
        val model = ExpenseDetailUiModel(
            title = "Dinner",
            notesText = notes
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpenseDetailScreen(
                    uiState = ExpenseDetailUiState(expense = model, isLoading = false)
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(notes).assertIsDisplayed()
        composeRule.onNodeWithText("\"$notes\"").assertDoesNotExist()
    }

    @Test
    fun rendersSplitBreakdown_withSoloMembers_whenNoSubunitsExist() {
        val aliceSplit = SplitDetailUiModel(
            displayName = "Alice",
            formattedAmount = "15,00 €",
            memberDisplay = MemberDisplay.Active("1", "Alice")
        )
        val bobSplit = SplitDetailUiModel(
            displayName = "Bob",
            formattedAmount = "15,00 €",
            memberDisplay = MemberDisplay.Active("2", "Bob")
        )
        val model = ExpenseDetailUiModel(
            title = "Lunch",
            splitTypeText = "Equal",
            splitBreakdownItems = persistentListOf(
                SplitBreakdownItemUiModel.Solo(aliceSplit),
                SplitBreakdownItemUiModel.Solo(bobSplit)
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpenseDetailScreen(
                    uiState = ExpenseDetailUiState(expense = model, isLoading = false)
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun rendersSplitBreakdown_withSubunits_andExpandsSubunitMembersOnTap() {
        val charlieSplit = SplitDetailUiModel(
            displayName = "Charlie",
            formattedAmount = "20,00 €",
            memberDisplay = MemberDisplay.Active("3", "Charlie")
        )
        val subunitGroup = SubunitSplitGroupUiModel(
            subunitId = "sub-1",
            subunitLabel = "Alpha Subunit",
            formattedTotalAmount = "20,00 €",
            memberCount = 1,
            members = persistentListOf(charlieSplit),
            splitTypeText = "Equal"
        )
        val model = ExpenseDetailUiModel(
            title = "Hotel",
            splitTypeText = "Custom",
            splitBreakdownItems = persistentListOf(
                SplitBreakdownItemUiModel.Subunit(subunitGroup)
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpenseDetailScreen(
                    uiState = ExpenseDetailUiState(expense = model, isLoading = false)
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Alpha Subunit").assertIsDisplayed()
        composeRule.onNodeWithText("Charlie").assertDoesNotExist()

        composeRule.onNodeWithText("Alpha Subunit").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Charlie").assertIsDisplayed()
    }
}
