package es.pedrazamiguez.splittrip.features.settlement.presentation.feature

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.YourBalanceContent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.YourBalanceUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.YourBalanceUiState

@Composable
internal fun YourBalanceFeatureBody(
    uiState: YourBalanceUiState,
    onEvent: (YourBalanceUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val personalPosition = uiState.personalPosition ?: return
    YourBalanceContent(
        personalPosition = personalPosition,
        isCashBreakdownVisible = uiState.isCashBreakdownVisible,
        settlementConsensus = uiState.settlementConsensus,
        chart = uiState.spendingChart,
        isCashOnly = uiState.isChartCashOnly,
        isOffline = uiState.isOffline,
        onEvent = onEvent,
        modifier = modifier.fillMaxSize()
    )
}
