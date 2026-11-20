package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ToolsKitchen2
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.CategorySpendingScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.CategorySpendingUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for empty, filtered-empty, and populated states on [CategorySpendingScreen].
 */
@RunWith(AndroidJUnit4::class)
class CategorySpendingScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersCategorySpendingScreen_emptyState_displaysCategorySpendingEmptyCopy() {
        val emptyTitle = context.getString(R.string.expenses_category_spending_empty_title)
        val emptyDescription = context.getString(R.string.expenses_category_spending_empty_description)
        val totalSpentLabel = context.getString(R.string.expenses_category_spending_total_spent)

        composeRule.setContent {
            SplitTripTheme {
                CategorySpendingScreen(
                    uiState = CategorySpendingUiState(
                        isLoading = false,
                        items = persistentListOf(),
                        isFiltered = false
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
        composeRule.onNodeWithText(totalSpentLabel).assertDoesNotExist()
    }

    @Test
    fun rendersCategorySpendingScreen_filteredEmptyState_displaysFilteredEmptyCopy() {
        val emptyTitle = context.getString(R.string.expenses_category_spending_filtered_empty_title)
        val emptyDescription = context.getString(R.string.expenses_category_spending_filtered_empty_description)
        val totalSpentLabel = context.getString(R.string.expenses_category_spending_total_spent)

        composeRule.setContent {
            SplitTripTheme {
                CategorySpendingScreen(
                    uiState = CategorySpendingUiState(
                        isLoading = false,
                        items = persistentListOf(),
                        isFiltered = true
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
        composeRule.onNodeWithText(totalSpentLabel).assertDoesNotExist()
    }

    @Test
    fun rendersCategorySpendingScreen_nonEmptyState_displaysChartAndCategoryBreakdown() {
        val categoryName = "Food"
        val itemAmount = "100.00 €"
        val totalAmount = "150.00 €"

        composeRule.setContent {
            SplitTripTheme {
                CategorySpendingScreen(
                    uiState = CategorySpendingUiState(
                        isLoading = false,
                        items = persistentListOf(
                            CategorySpendingUiModel(
                                categoryName = categoryName,
                                categoryIcon = TablerIcons.Outline.ToolsKitchen2,
                                formattedAmount = itemAmount,
                                formattedPercentage = "67%",
                                progress = 0.67f,
                                color = Color.Blue
                            )
                        ),
                        totalFormattedAmount = totalAmount
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(totalAmount).assertIsDisplayed()
        composeRule.onNodeWithText(categoryName).assertIsDisplayed()
    }
}
