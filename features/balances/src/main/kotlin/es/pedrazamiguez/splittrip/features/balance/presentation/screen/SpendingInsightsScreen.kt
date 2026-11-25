package es.pedrazamiguez.splittrip.features.balance.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DashboardShimmer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerBox
import es.pedrazamiguez.splittrip.features.balance.presentation.component.export.TripReportExportBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.component.insights.SpendingInsightsBodyContent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState

@Composable
fun SpendingInsightsScreen(
    uiState: SpendingInsightsUiState = SpendingInsightsUiState(),
    onEvent: (SpendingInsightsUiEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current

    Box(modifier = modifier.fillMaxSize()) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = {
                DashboardShimmer(
                    modifier = Modifier.padding(top = topPadding),
                    bottomPadding = bottomPadding,
                    headerContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                            ShimmerBox(height = 32.dp, width = 220.dp)
                            ShimmerBox(height = 14.dp, width = 180.dp)
                        }
                    }
                )
            }
        ) {
            SpendingInsightsBodyContent(
                uiState = uiState,
                onEvent = onEvent
            )
        }
    }

    if (uiState.exportSheetState.isVisible) {
        TripReportExportBottomSheet(
            sheetState = uiState.exportSheetState,
            onSelectPreset = { onEvent(SpendingInsightsUiEvent.SelectExportPeriodPreset(it)) },
            onSelectCustomStartDate = { onEvent(SpendingInsightsUiEvent.SelectExportCustomStartDate(it)) },
            onSelectCustomEndDate = { onEvent(SpendingInsightsUiEvent.SelectExportCustomEndDate(it)) },
            onSelectFormat = { onEvent(SpendingInsightsUiEvent.SelectExportFormat(it)) },
            onConfirmExport = { onEvent(SpendingInsightsUiEvent.ConfirmExportReport) },
            onSaveToDevice = { onEvent(SpendingInsightsUiEvent.SaveExportReport) },
            onDismiss = { onEvent(SpendingInsightsUiEvent.DismissExportReportSheet) }
        )
    }
}
