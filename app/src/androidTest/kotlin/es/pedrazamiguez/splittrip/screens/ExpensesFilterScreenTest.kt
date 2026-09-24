package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesFilterScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ExpensesFilterScreen].
 */
@RunWith(AndroidJUnit4::class)
class ExpensesFilterScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersResetFiltersDialog_whenResetClicked_andTriggersCallbackOnConfirm() {
        var resetFiltersInvoked = false
        val resetButton = context.getString(R.string.expenses_filter_reset)
        val dialogTitle = context.getString(R.string.expenses_filter_reset_dialog_title)
        val dialogConfirm = context.getString(R.string.expenses_filter_reset_dialog_confirm)

        val uiState = ExpensesFilterUiState(
            draftCriteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            ),
            matchingExpensesCount = 2,
            totalExpensesCount = 3
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onResetFilters = { resetFiltersInvoked = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(resetButton).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeRule.onNode(hasText(dialogConfirm).and(hasAnyAncestor(isDialog()))).performClick()
        composeRule.waitForIdle()

        assertTrue(resetFiltersInvoked)
    }

    @Test
    fun dismissesResetFiltersDialog_whenCancelClicked_andDoesNotTriggerCallback() {
        var resetFiltersInvoked = false
        val resetButton = context.getString(R.string.expenses_filter_reset)
        val dialogTitle = context.getString(R.string.expenses_filter_reset_dialog_title)
        val dialogDismiss = context.getString(R.string.expenses_filter_reset_dialog_dismiss)

        val uiState = ExpensesFilterUiState(
            draftCriteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            ),
            matchingExpensesCount = 2,
            totalExpensesCount = 3
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onResetFilters = { resetFiltersInvoked = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(resetButton).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeRule.onNodeWithText(dialogDismiss).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(dialogTitle).assertDoesNotExist()
        assertFalse(resetFiltersInvoked)
    }

    @Test
    fun disablesResetButton_whenCanResetIsFalse() {
        val resetButton = context.getString(R.string.expenses_filter_reset)

        val uiState = ExpensesFilterUiState(
            draftCriteria = ExpenseFilterCriteria()
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(resetButton).assertIsNotEnabled()
    }

    @Test
    fun triggersApplyFiltersCallback_whenApplyClicked() {
        var applyFiltersInvoked = false
        val applyButton = context.getString(R.string.expenses_filter_apply)

        val uiState = ExpensesFilterUiState(
            draftCriteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )
        )

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onApplyFilters = { applyFiltersInvoked = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(applyButton).performClick()
        composeRule.waitForIdle()

        assertTrue(applyFiltersInvoked)
    }
}
