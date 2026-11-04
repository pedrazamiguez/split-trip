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
    var selectedMetricInfo by remember { mutableStateOf<BalanceMetricType?>(null) }

    BalancesBodyContent(
        uiState = uiState,
        onEvent = onEvent,
        onNavigateToContribution = onNavigateToContribution,
        onNavigateToContributionDetail = onNavigateToContributionDetail,
        onNavigateToWithdrawal = onNavigateToWithdrawal,
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

    selectedMetricInfo?.let { metricType ->
        val (breakdown, grandTotal) = when (metricType) {
            BalanceMetricType.EXTRAS ->
                uiState.extrasBreakdown to (uiState.pocketBalance.formattedTotalExtras.orEmpty())

            BalanceMetricType.SCHEDULED ->
                uiState.scheduledBreakdown to (uiState.pocketBalance.formattedScheduledHoldAmount.orEmpty())

            BalanceMetricType.REFUNDABLE ->
                uiState.refundableBreakdown to (uiState.pocketBalance.formattedRefundableHoldAmount.orEmpty())

            else -> null to ""
        }
        BalanceMetricInfoBottomSheet(
            metricType = metricType,
            breakdown = breakdown,
            formattedGrandTotal = grandTotal,
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
