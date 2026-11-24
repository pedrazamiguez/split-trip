package es.pedrazamiguez.splittrip.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesFilterScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertEquals
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

    @Test
    fun selectingMultipleCategories_showsSingleContextualTray_andInlineSubcategoryCounts() {
        val transportLabel = context.getString(ExpenseCategory.TRANSPORT.toStringRes())
        val foodLabel = context.getString(ExpenseCategory.FOOD.toStringRes())
        val trainLabel = context.getString(ExpenseSubcategory.TRAIN.toStringRes())
        val taxiLabel = context.getString(ExpenseSubcategory.TAXI_RIDESHARE.toStringRes())
        val transportTrayTitle = context.getString(R.string.category_subcategories_tray_title, transportLabel)
        val foodTrayTitle = context.getString(R.string.category_subcategories_tray_title, foodLabel)

        composeRule.setContent {
            var uiState by remember { mutableStateOf(ExpensesFilterUiState()) }
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onUpdateDraft = { criteria -> uiState = uiState.copy(draftCriteria = criteria) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(transportLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(transportTrayTitle).assertIsDisplayed()

        composeRule.onNodeWithText(trainLabel).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(taxiLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(foodLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(foodTrayTitle).assertIsDisplayed()
        composeRule.onNodeWithText(transportTrayTitle).assertDoesNotExist()
        composeRule.onNode(
            hasText(transportLabel).and(hasAnyDescendant(hasText("2")))
        ).assertIsDisplayed().assertIsSelected()
        composeRule.onAllNodesWithText("2").assertCountEquals(2)
    }

    @Test
    fun tappingSelectedUnfocusedCategory_focusesItsTray_andSecondTapDeselectsAndClearsChildren() {
        val transportLabel = context.getString(ExpenseCategory.TRANSPORT.toStringRes())
        val foodLabel = context.getString(ExpenseCategory.FOOD.toStringRes())
        val transportTrayTitle = context.getString(R.string.category_subcategories_tray_title, transportLabel)
        val foodTrayTitle = context.getString(R.string.category_subcategories_tray_title, foodLabel)

        var latestCriteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD),
            selectedSubcategories = setOf(
                ExpenseSubcategory.TRAIN,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        )

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(ExpensesFilterUiState(draftCriteria = latestCriteria))
            }
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onUpdateDraft = { criteria ->
                        latestCriteria = criteria
                        uiState = uiState.copy(draftCriteria = criteria)
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(foodLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(foodTrayTitle).assertIsDisplayed()

        composeRule.onNodeWithText(transportLabel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(transportTrayTitle).assertIsDisplayed()
        assertEquals(setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD), latestCriteria.selectedCategories)
        assertEquals(
            setOf(ExpenseSubcategory.TRAIN, ExpenseSubcategory.TAXI_RIDESHARE),
            latestCriteria.selectedSubcategories
        )

        Thread.sleep(UiConstants.DEFAULT_DEBOUNCE_MS + 100L)

        composeRule.onNodeWithText(transportLabel).performClick()
        composeRule.waitForIdle()

        assertEquals(setOf(ExpenseCategory.FOOD), latestCriteria.selectedCategories)
        assertTrue(latestCriteria.selectedSubcategories.isEmpty())
        composeRule.onNodeWithText(foodTrayTitle).assertIsDisplayed()
    }

    @Test
    fun togglingShowAllCategories_invokesCallback() {
        var toggleInvoked = false
        val showAllLabel = context.getString(R.string.category_show_all_toggle)

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = ExpensesFilterUiState(),
                    onToggleShowAllCategories = { toggleInvoked = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(showAllLabel).performClick()
        composeRule.waitForIdle()

        assertTrue(toggleInvoked)
    }
}
