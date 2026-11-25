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
import androidx.compose.ui.test.isSelected
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
import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesFilterScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
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

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    ExpensesFilterUiState(
                        availableCategories = persistentListOf(
                            ExpenseCategory.TRANSPORT,
                            ExpenseCategory.FOOD
                        ),
                        availableSubcategories = persistentListOf(
                            ExpenseSubcategory.TRAIN,
                            ExpenseSubcategory.TAXI_RIDESHARE,
                            ExpenseSubcategory.RESTAURANT
                        )
                    )
                )
            }
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
        composeRule.onAllNodesWithText(transportLabel).assertCountEquals(2)

        composeRule.onNodeWithText(trainLabel).performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(taxiLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(foodLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(foodLabel).assertCountEquals(2)
        composeRule.onAllNodesWithText(transportLabel).assertCountEquals(1)
        composeRule.onNode(
            hasText(transportLabel).and(hasAnyDescendant(hasText("2")))
        ).assertIsDisplayed().assertIsSelected()
        composeRule.onAllNodesWithText("2").assertCountEquals(2)
    }

    @Test
    fun tappingSelectedUnfocusedCategory_focusesItsTray_andSecondTapDeselectsAndClearsChildren() {
        val transportLabel = context.getString(ExpenseCategory.TRANSPORT.toStringRes())
        val foodLabel = context.getString(ExpenseCategory.FOOD.toStringRes())

        var latestCriteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD),
            selectedSubcategories = setOf(
                ExpenseSubcategory.TRAIN,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        )

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    ExpensesFilterUiState(
                        draftCriteria = latestCriteria,
                        availableCategories = persistentListOf(
                            ExpenseCategory.TRANSPORT,
                            ExpenseCategory.FOOD
                        ),
                        availableSubcategories = persistentListOf(
                            ExpenseSubcategory.TRAIN,
                            ExpenseSubcategory.TAXI_RIDESHARE,
                            ExpenseSubcategory.RESTAURANT
                        )
                    )
                )
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
        composeRule.onAllNodesWithText(foodLabel).assertCountEquals(2)

        composeRule.onNode(hasText(transportLabel).and(isSelected())).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(transportLabel).assertCountEquals(2)
        assertEquals(setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD), latestCriteria.selectedCategories)
        assertEquals(
            setOf(ExpenseSubcategory.TRAIN, ExpenseSubcategory.TAXI_RIDESHARE),
            latestCriteria.selectedSubcategories
        )

        Thread.sleep(UiConstants.DEFAULT_DEBOUNCE_MS + 100L)

        composeRule.onNode(hasText(transportLabel).and(isSelected())).performClick()
        composeRule.waitForIdle()

        assertEquals(setOf(ExpenseCategory.FOOD), latestCriteria.selectedCategories)
        assertTrue(latestCriteria.selectedSubcategories.isEmpty())
        composeRule.onAllNodesWithText(foodLabel).assertCountEquals(2)
    }

    @Test
    fun rendersOnlyAvailableCategoriesAndSubcategories_andHidesUnusedOnes() {
        val foodLabel = context.getString(ExpenseCategory.FOOD.toStringRes())
        val transportLabel = context.getString(ExpenseCategory.TRANSPORT.toStringRes())
        val lodgingLabel = context.getString(ExpenseCategory.LODGING.toStringRes())
        val showAllLabel = context.getString(R.string.category_show_all_toggle)
        val restaurantLabel = context.getString(ExpenseSubcategory.RESTAURANT.toStringRes())
        val cafeBreakfastLabel = context.getString(ExpenseSubcategory.CAFE_BREAKFAST.toStringRes())

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    ExpensesFilterUiState(
                        availableCategories = persistentListOf(ExpenseCategory.FOOD),
                        availableSubcategories = persistentListOf(ExpenseSubcategory.RESTAURANT)
                    )
                )
            }
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onUpdateDraft = { criteria -> uiState = uiState.copy(draftCriteria = criteria) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(foodLabel).assertIsDisplayed()
        composeRule.onNodeWithText(transportLabel).assertDoesNotExist()
        composeRule.onNodeWithText(lodgingLabel).assertDoesNotExist()
        composeRule.onNodeWithText(showAllLabel).assertDoesNotExist()

        composeRule.onNodeWithText(foodLabel).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(restaurantLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(cafeBreakfastLabel).assertDoesNotExist()
    }

    @Test
    fun hidesCategoryFilterSection_whenNoAvailableOrSelectedCategories() {
        val categorySectionTitle = context.getString(R.string.expenses_filter_section_category)

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = ExpensesFilterUiState()
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(categorySectionTitle).assertDoesNotExist()
    }

    @Test
    fun retainsPreSelectedMissingCategoryAndSubcategory_soUserCanDeselect() {
        val entertainmentLabel = context.getString(ExpenseCategory.ENTERTAINMENT.toStringRes())
        val concertLabel = context.getString(ExpenseSubcategory.CONCERT_FESTIVAL.toStringRes())

        composeRule.setContent {
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = ExpensesFilterUiState(
                        availableCategories = persistentListOf(ExpenseCategory.FOOD),
                        availableSubcategories = persistentListOf(ExpenseSubcategory.RESTAURANT),
                        draftCriteria = ExpenseFilterCriteria(
                            selectedCategories = setOf(ExpenseCategory.ENTERTAINMENT),
                            selectedSubcategories = setOf(ExpenseSubcategory.CONCERT_FESTIVAL)
                        )
                    )
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(hasText(entertainmentLabel).and(isSelected())).assertIsDisplayed()
        composeRule.onNode(hasText(concertLabel).and(isSelected())).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectingNonCategoryFilters_displaysCountBadgesAndSelectedChips() {
        val cashLabel = context.getString(PaymentMethod.CASH.toStringRes())
        val bizumLabel = context.getString(PaymentMethod.BIZUM.toStringRes())
        val paidLabel = context.getString(PaymentStatus.FINISHED.toStringRes())
        val hasReceiptLabel = context.getString(ExpenseAttribute.HAS_RECEIPT.toStringRes())

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    ExpensesFilterUiState(
                        draftCriteria = ExpenseFilterCriteria(
                            selectedPaymentMethods = setOf(PaymentMethod.CASH),
                            selectedPaymentStatuses = setOf(PaymentStatus.FINISHED),
                            selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT)
                        )
                    )
                )
            }
            SplitTripTheme {
                ExpensesFilterScreen(
                    uiState = uiState,
                    onUpdateDraft = { criteria -> uiState = uiState.copy(draftCriteria = criteria) }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(cashLabel).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(bizumLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(bizumLabel).assertIsSelected()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText(paidLabel).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(hasReceiptLabel).performScrollTo().assertIsSelected()
        composeRule.onAllNodesWithText("1").assertCountEquals(2)
    }
}
