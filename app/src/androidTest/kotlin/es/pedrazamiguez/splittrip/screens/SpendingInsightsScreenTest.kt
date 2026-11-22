package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ToolsKitchen2
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.AddOnBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.AddOnTypeBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingBurnRateUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategoryInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.SpendingInsightsScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for empty, filtered-empty, and populated states on [SpendingInsightsScreen].
 */
@RunWith(AndroidJUnit4::class)
class SpendingInsightsScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersSpendingInsightsScreen_emptyState_displaysEmptyCopy() {
        val emptyTitle = context.getString(R.string.balances_insights_empty_title)
        val emptyDescription = context.getString(R.string.balances_insights_empty_description)

        composeRule.setContent {
            SplitTripTheme {
                SpendingInsightsScreen(
                    uiState = SpendingInsightsUiState(
                        isLoading = false,
                        hasAnyGroupExpenses = false,
                        isFiltered = false
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
    }

    @Test
    fun rendersSpendingInsightsScreen_filteredEmptyState_displaysBannerAndFilteredEmptyCopy() {
        val bannerTitle = context.getString(R.string.balances_insights_filtered_banner_title)
        val emptyTitle = context.getString(R.string.balances_insights_empty_filtered_title)
        val emptyDescription = context.getString(R.string.balances_insights_empty_filtered_description)

        composeRule.setContent {
            SplitTripTheme {
                SpendingInsightsScreen(
                    uiState = SpendingInsightsUiState(
                        isLoading = false,
                        hasAnyGroupExpenses = false,
                        isFiltered = true,
                        activeFilterCount = 1
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(bannerTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
    }

    @Test
    fun rendersSpendingInsightsScreen_populatedWithAddOnsAndSubcategories_expandsSubcategoryOnTap() {
        val categoryName = "Food"
        val subcategoryName = "Restaurant"
        val addOnsTitle = context.getString(R.string.balances_insights_add_ons_title)

        composeRule.setContent {
            SplitTripTheme {
                SpendingInsightsScreen(
                    uiState = SpendingInsightsUiState(
                        isLoading = false,
                        hasAnyGroupExpenses = true,
                        hasPeriodExpenses = true,
                        burnRate = SpendingBurnRateUiModel(
                            formattedTotalSpent = "150.00 €",
                            addOnsBreakdown = AddOnBreakdownUiModel(
                                formattedTotalExtras = "12.00 €",
                                formattedPercentageOfSpending = "8% of spend",
                                typeBreakdown = persistentListOf(
                                    AddOnTypeBreakdownUiModel(
                                        type = AddOnType.FEE,
                                        label = "Fees",
                                        icon = AddOnType.FEE.toIconVector(),
                                        formattedAmount = "12.00 €"
                                    )
                                )
                            ),
                            formattedDailyAverage = "30.00 € / day",
                            activeDaysLabel = "3 active days",
                            formattedMonthlyPace = null,
                            monthlyPaceLabel = null,
                            formattedPeakDayAmount = "80.00 €",
                            formattedPeakDayDate = "12 Nov"
                        ),
                        categoryItems = persistentListOf(
                            CategoryInsightUiModel(
                                category = ExpenseCategory.FOOD,
                                categoryName = categoryName,
                                categoryIcon = TablerIcons.Outline.ToolsKitchen2,
                                formattedAmount = "150.00 €",
                                formattedPercentage = "100%",
                                expenseCountLabel = "3 expenses",
                                progress = 1f,
                                color = ChartColors[0],
                                subcategories = persistentListOf(
                                    SubcategoryInsightUiModel(
                                        subcategory = ExpenseSubcategory.RESTAURANT,
                                        subcategoryName = subcategoryName,
                                        subcategoryIcon = ExpenseSubcategory.RESTAURANT.toIconVector(),
                                        formattedAmount = "150.00 €",
                                        formattedPercentage = "100%",
                                        expenseCountLabel = "3 expenses"
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(addOnsTitle).assertIsDisplayed()
        composeRule.onNodeWithText(categoryName).assertIsDisplayed()
        composeRule.onNodeWithText(subcategoryName).assertDoesNotExist()

        composeRule.onNodeWithText(categoryName).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(subcategoryName).assertIsDisplayed()
    }
}
