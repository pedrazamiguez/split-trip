package es.pedrazamiguez.splittrip.features.balance.presentation.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.rememberConnectedScrollBehavior
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesBodyContent
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesScreenOverlays
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ExtrasBreakdownBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.component.SettlementsBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.component.WithdrawalDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    uiState: BalancesUiState = BalancesUiState(),
    onEvent: (BalancesUiEvent) -> Unit = {},
    onNavigateToContribution: () -> Unit = {},
    onNavigateToWithdrawal: () -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current
    val scrollBehavior = rememberConnectedScrollBehavior()

    var contributionPendingDelete by remember { mutableStateOf<ContributionUiModel?>(null) }
    var withdrawalPendingDelete by remember { mutableStateOf<CashWithdrawalUiModel?>(null) }
    var showExtrasBreakdown by remember { mutableStateOf(false) }
    var showSettlementsBottomSheet by remember { mutableStateOf(false) }

    BalancesBodyContent(
        uiState = uiState,
        bottomPadding = bottomPadding,
        onEvent = onEvent,
        onNavigateToContribution = onNavigateToContribution,
        onNavigateToWithdrawal = onNavigateToWithdrawal,
        onShowExtrasBreakdown = { showExtrasBreakdown = true },
        onSimplifyDebts = { showSettlementsBottomSheet = true },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    )

    BalancesScreenOverlays(
        uiState = uiState,
        onEvent = onEvent,
        onContributionDeleteRequested = { contributionPendingDelete = it },
        onWithdrawalDeleteRequested = { withdrawalPendingDelete = it }
    )

    if (showSettlementsBottomSheet) {
        SettlementsBottomSheet(
            settlements = uiState.settlements,
            onDismiss = { showSettlementsBottomSheet = false }
        )
    }

    if (showExtrasBreakdown) {
        ExtrasBreakdownBottomSheet(
            breakdown = uiState.extrasBreakdown,
            formattedGrandTotal = uiState.pocketBalance.formattedTotalExtras ?: "",
            onDismiss = { showExtrasBreakdown = false }
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
