package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartBar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState

@Composable
internal fun SpendingInsightsBodyContent(
    uiState: SpendingInsightsUiState,
    onEvent: (SpendingInsightsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current

    if (!uiState.hasAnyGroupExpenses && !uiState.isFiltered) {
        EmptyStateView(
            title = stringResource(R.string.balances_insights_empty_title),
            description = stringResource(R.string.balances_insights_empty_description),
            icon = TablerIcons.Outline.ChartBar,
            modifier = modifier.padding(top = topPadding, bottom = bottomPadding)
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.Default + topPadding,
            end = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        if (uiState.isFiltered) {
            item(key = "active-filter-banner") {
                SpendingInsightsFilterBanner(
                    activeFilterCount = uiState.activeFilterCount,
                    onClearFilters = { onEvent(SpendingInsightsUiEvent.ClearExpenseFilters) }
                )
            }
        }

        if (!uiState.hasAnyGroupExpenses) {
            filteredEmptySection()
        } else {
            periodSelectorSection(uiState = uiState, onEvent = onEvent)

            // Slot reserved right beneath the period selector for #2023 Budget Pacing card.

            if (!uiState.hasPeriodExpenses) {
                periodEmptySection()
            } else {
                insightsSections(uiState = uiState, onEvent = onEvent)
            }
        }
    }
}

private fun LazyListScope.periodSelectorSection(
    uiState: SpendingInsightsUiState,
    onEvent: (SpendingInsightsUiEvent) -> Unit
) {
    item(key = "period-selector") {
        SpendingInsightsPeriodCard(
            selectedPreset = uiState.selectedPreset,
            periodSummaryLabel = uiState.periodSummaryLabel,
            customStartDate = uiState.customStartDate,
            customEndDate = uiState.customEndDate,
            formattedCustomStartDate = uiState.formattedCustomStartDate,
            formattedCustomEndDate = uiState.formattedCustomEndDate,
            oldestExpenseDate = uiState.oldestExpenseDate,
            newestExpenseDate = uiState.newestExpenseDate,
            onSelectPreset = { preset -> onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(preset)) },
            onSelectCustomStartDate = { date -> onEvent(SpendingInsightsUiEvent.SelectCustomStartDate(date)) },
            onSelectCustomEndDate = { date -> onEvent(SpendingInsightsUiEvent.SelectCustomEndDate(date)) }
        )
    }
}

private fun LazyListScope.filteredEmptySection() {
    item(key = "filtered-empty") {
        EmptyStateView(
            title = stringResource(R.string.balances_insights_empty_filtered_title),
            description = stringResource(R.string.balances_insights_empty_filtered_description),
            icon = TablerIcons.Outline.Search,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun LazyListScope.periodEmptySection() {
    item(key = "period-empty") {
        EmptyStateView(
            title = stringResource(R.string.balances_insights_empty_period_title),
            description = stringResource(R.string.balances_insights_empty_period_description),
            icon = TablerIcons.Outline.Calendar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun LazyListScope.insightsSections(
    uiState: SpendingInsightsUiState,
    onEvent: (SpendingInsightsUiEvent) -> Unit
) {
    uiState.burnRate?.let { burnRate ->
        item(key = "burn-rate") {
            SpendingBurnRateCard(burnRate = burnRate)
        }
        if (burnRate.dailyBars.isNotEmpty()) {
            item(key = "daily-trend") {
                DailySpendingTrendCard(
                    dailyBars = burnRate.dailyBars,
                    selectedBarId = uiState.selectedDayBarId,
                    onSelectBar = { barId -> onEvent(SpendingInsightsUiEvent.SelectDayBar(barId)) }
                )
            }
        }
    }

    if (uiState.categoryItems.isNotEmpty()) {
        item(key = "category-breakdown") {
            CategoryBreakdownCard(
                items = uiState.categoryItems,
                totalFormattedAmount = uiState.burnRate?.formattedTotalSpent.orEmpty()
            )
        }
    }

    val activeBreakdownItems = if (
        uiState.hasSubunits && uiState.entityBreakdownMode == EntityBreakdownMode.BY_SUBUNIT
    ) {
        uiState.entityItems
    } else {
        uiState.memberItems
    }

    if (activeBreakdownItems.isNotEmpty()) {
        item(key = "entity-breakdown") {
            EntitySpendingBreakdownCard(
                hasSubunits = uiState.hasSubunits,
                selectedMode = uiState.entityBreakdownMode,
                items = activeBreakdownItems,
                onSelectMode = { mode -> onEvent(SpendingInsightsUiEvent.SelectEntityBreakdownMode(mode)) }
            )
        }
    }
}
