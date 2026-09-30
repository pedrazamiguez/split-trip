package es.pedrazamiguez.splittrip.features.balance.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalanceMetricInfoBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesBodyContent
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesScreenOverlays
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ExtrasBreakdownBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.component.WithdrawalDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@Composable
fun BalancesScreen(
    uiState: BalancesUiState = BalancesUiState(),
    onEvent: (BalancesUiEvent) -> Unit = {},
    onNavigateToContribution: () -> Unit = {},
    onNavigateToContributionDetail: (String) -> Unit = {},
    onNavigateToEditContribution: (String) -> Unit = {},
    onNavigateToWithdrawal: () -> Unit = {}
) {
    var contributionPendingDelete by remember { mutableStateOf<ContributionUiModel?>(null) }
    var withdrawalPendingDelete by remember { mutableStateOf<CashWithdrawalUiModel?>(null) }
    var showExtrasBreakdown by remember { mutableStateOf(false) }
    var selectedMetricInfo by remember { mutableStateOf<BalanceMetricType?>(null) }

    BalancesBodyContent(
        uiState = uiState,
        onEvent = onEvent,
        onNavigateToContribution = onNavigateToContribution,
        onNavigateToContributionDetail = onNavigateToContributionDetail,
        onNavigateToWithdrawal = onNavigateToWithdrawal,
        onShowExtrasBreakdown = { showExtrasBreakdown = true },
        onShowMetricInfo = { selectedMetricInfo = it },
        modifier = Modifier
    )

    BalancesScreenOverlays(
        uiState = uiState,
        onEvent = onEvent,
        onNavigateToEditContribution = onNavigateToEditContribution,
        onContributionDeleteRequested = { contributionPendingDelete = it },
        onWithdrawalDeleteRequested = { withdrawalPendingDelete = it }
    )

    if (showExtrasBreakdown) {
        ExtrasBreakdownBottomSheet(
            breakdown = uiState.extrasBreakdown,
            formattedGrandTotal = uiState.pocketBalance.formattedTotalExtras ?: "",
            onDismiss = { showExtrasBreakdown = false }
        )
    }

    selectedMetricInfo?.let { metricType ->
        BalanceMetricInfoBottomSheet(
            metricType = metricType,
            onDismiss = { selectedMetricInfo = null }
        )
    }

    contributionPendingDelete?.let { contribution ->
        ContributionDeleteDialog(
            contribution = contribution,
            onDismiss = { contributionPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteContributionConfirmed(contribution.id))
                contributionPendingDelete = null
            }
        )
    }

    withdrawalPendingDelete?.let { withdrawal ->
        WithdrawalDeleteDialog(
            withdrawal = withdrawal,
            onDismiss = { withdrawalPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteWithdrawalConfirmed(withdrawal.id))
                withdrawalPendingDelete = null
            }
        )
    }
}
