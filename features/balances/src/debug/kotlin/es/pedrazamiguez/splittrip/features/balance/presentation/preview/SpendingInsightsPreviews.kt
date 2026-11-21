package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bed
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Route
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ToolsKitchen2
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.features.balance.presentation.component.SpendingInsightsEntryCard
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.DailySpendingBarUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntitySpendingInsightUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingBurnRateUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.SpendingInsightsScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_DAILY_BARS = persistentListOf(
    DailySpendingBarUiModel(
        id = "2026-11-10",
        axisLabel = "10",
        fullDateLabel = "10 Nov",
        formattedAmount = "€42.00",
        expenseCountLabel = "1 expense",
        heightRatio = 0.28f,
        isPeakDay = false,
        contentDescription = "10 Nov, €42.00"
    ),
    DailySpendingBarUiModel(
        id = "2026-11-11",
        axisLabel = "11",
        fullDateLabel = "11 Nov",
        formattedAmount = "€0.00",
        expenseCountLabel = "0 expenses",
        heightRatio = 0f,
        isPeakDay = false,
        contentDescription = "11 Nov, €0.00"
    ),
    DailySpendingBarUiModel(
        id = "2026-11-12",
        axisLabel = "12",
        fullDateLabel = "12 Nov",
        formattedAmount = "€150.00",
        expenseCountLabel = "2 expenses",
        heightRatio = 1.0f,
        isPeakDay = true,
        contentDescription = "12 Nov, €150.00"
    ),
    DailySpendingBarUiModel(
        id = "2026-11-13",
        axisLabel = "13",
        fullDateLabel = "13 Nov",
        formattedAmount = "€85.00",
        expenseCountLabel = "2 expenses",
        heightRatio = 0.57f,
        isPeakDay = false,
        contentDescription = "13 Nov, €85.00"
    ),
    DailySpendingBarUiModel(
        id = "2026-11-14",
        axisLabel = "14",
        fullDateLabel = "14 Nov",
        formattedAmount = "€63.00",
        expenseCountLabel = "1 expense",
        heightRatio = 0.42f,
        isPeakDay = false,
        contentDescription = "14 Nov, €63.00"
    )
)

private val PREVIEW_BURN_RATE = SpendingBurnRateUiModel(
    formattedTotalSpent = "€340.00",
    formattedExtrasCaption = "Includes €15.00 in extras",
    formattedDailyAverage = "€68.00 / day",
    activeDaysLabel = "4 active days",
    formattedMonthlyPace = "€1,420.00 / mo",
    monthlyPaceLabel = "Projected monthly pace",
    formattedPeakDayAmount = "€150.00",
    formattedPeakDayDate = "12 Nov",
    dailyBars = PREVIEW_DAILY_BARS,
    defaultSelectedBarId = "2026-11-12"
)

private val PREVIEW_CATEGORIES = persistentListOf(
    CategoryInsightUiModel(
        category = ExpenseCategory.FOOD,
        categoryName = "Food",
        categoryIcon = TablerIcons.Outline.ToolsKitchen2,
        formattedAmount = "€170.00",
        formattedPercentage = "50%",
        expenseCountLabel = "3 expenses",
        progress = 0.50f,
        color = ChartColors[0]
    ),
    CategoryInsightUiModel(
        category = ExpenseCategory.LODGING,
        categoryName = "Lodging",
        categoryIcon = TablerIcons.Outline.Bed,
        formattedAmount = "€102.00",
        formattedPercentage = "30%",
        expenseCountLabel = "1 expense",
        progress = 0.30f,
        color = ChartColors[1]
    ),
    CategoryInsightUiModel(
        category = ExpenseCategory.TRANSPORT,
        categoryName = "Transport",
        categoryIcon = TablerIcons.Outline.Route,
        formattedAmount = "€68.00",
        formattedPercentage = "20%",
        expenseCountLabel = "2 expenses",
        progress = 0.20f,
        color = ChartColors[2]
    )
)

private val PREVIEW_ENTITIES = persistentListOf(
    EntitySpendingInsightUiModel(
        id = "subunit-1",
        displayName = "Alex & Sam",
        subtitle = "2 members",
        isSubunit = true,
        isCurrentUser = true,
        formattedAmount = "€210.00",
        formattedPercentage = "62%",
        progress = 0.62f,
        color = ChartColors[0]
    ),
    EntitySpendingInsightUiModel(
        id = "user-3",
        displayName = "Carlos",
        subtitle = "4 expenses",
        isSubunit = false,
        isCurrentUser = false,
        formattedAmount = "€130.00",
        formattedPercentage = "38%",
        progress = 0.38f,
        color = ChartColors[1]
    )
)

private val PREVIEW_MEMBERS = persistentListOf(
    EntitySpendingInsightUiModel(
        id = "user-1",
        displayName = "You",
        subtitle = "6 expenses",
        isSubunit = false,
        isCurrentUser = true,
        formattedAmount = "€110.00",
        formattedPercentage = "32%",
        progress = 0.32f,
        color = ChartColors[0]
    ),
    EntitySpendingInsightUiModel(
        id = "user-2",
        displayName = "Sam",
        subtitle = "6 expenses",
        isSubunit = false,
        isCurrentUser = false,
        formattedAmount = "€100.00",
        formattedPercentage = "30%",
        progress = 0.30f,
        color = ChartColors[1]
    ),
    EntitySpendingInsightUiModel(
        id = "user-3",
        displayName = "Carlos",
        subtitle = "4 expenses",
        isSubunit = false,
        isCurrentUser = false,
        formattedAmount = "€130.00",
        formattedPercentage = "38%",
        progress = 0.38f,
        color = ChartColors[2]
    )
)

@PreviewComplete
@Composable
private fun SpendingInsightsScreenLoadingPreview() {
    PreviewThemeWrapper {
        SpendingInsightsScreen(
            uiState = SpendingInsightsUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun SpendingInsightsScreenEmptyPreview() {
    PreviewThemeWrapper {
        SpendingInsightsScreen(
            uiState = SpendingInsightsUiState(
                isLoading = false,
                groupId = "group-1",
                hasAnyGroupExpenses = false
            )
        )
    }
}

@PreviewComplete
@Composable
private fun SpendingInsightsScreenEmptyPeriodPreview() {
    PreviewThemeWrapper {
        SpendingInsightsScreen(
            uiState = SpendingInsightsUiState(
                isLoading = false,
                groupId = "group-1",
                selectedPreset = SpendingPeriodPreset.LAST_MONTH,
                periodSummaryLabel = "October 2026",
                hasAnyGroupExpenses = true,
                hasPeriodExpenses = false
            )
        )
    }
}

@PreviewComplete
@Composable
private fun SpendingInsightsScreenWithDataPreview() {
    PreviewThemeWrapper {
        SpendingInsightsScreen(
            uiState = SpendingInsightsUiState(
                isLoading = false,
                groupId = "group-1",
                selectedPreset = SpendingPeriodPreset.ALL_TIME,
                periodSummaryLabel = "10 Nov – 14 Nov · 6 expenses",
                hasAnyGroupExpenses = true,
                hasPeriodExpenses = true,
                burnRate = PREVIEW_BURN_RATE,
                selectedDayBarId = "2026-11-12",
                categoryItems = PREVIEW_CATEGORIES,
                hasSubunits = true,
                entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
                entityItems = PREVIEW_ENTITIES,
                memberItems = PREVIEW_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
private fun SpendingInsightsScreenCustomRangePreview() {
    PreviewThemeWrapper {
        SpendingInsightsScreen(
            uiState = SpendingInsightsUiState(
                isLoading = false,
                groupId = "group-1",
                selectedPreset = SpendingPeriodPreset.CUSTOM,
                periodSummaryLabel = "10 Nov – 14 Nov · 6 expenses",
                customStartDate = LocalDate.of(2026, 11, 10),
                customEndDate = LocalDate.of(2026, 11, 14),
                formattedCustomStartDate = "10 Nov",
                formattedCustomEndDate = "14 Nov",
                oldestExpenseDate = LocalDate.of(2026, 11, 1),
                newestExpenseDate = LocalDate.of(2026, 11, 14),
                hasAnyGroupExpenses = true,
                hasPeriodExpenses = true,
                burnRate = PREVIEW_BURN_RATE,
                selectedDayBarId = "2026-11-12",
                categoryItems = PREVIEW_CATEGORIES,
                hasSubunits = false,
                entityBreakdownMode = EntityBreakdownMode.BY_MEMBER,
                entityItems = PREVIEW_MEMBERS,
                memberItems = PREVIEW_MEMBERS
            )
        )
    }
}

@PreviewComplete
@Composable
private fun SpendingInsightsEntryCardPreview() {
    PreviewThemeWrapper {
        SpendingInsightsEntryCard(
            onClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.Default)
        )
    }
}
