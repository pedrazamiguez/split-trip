package es.pedrazamiguez.splittrip.features.balance.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesBodyContent
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalancesScreenOverlays
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@Composable
fun BalancesScreen(
    uiState: BalancesUiState = BalancesUiState(),
    onEvent: (BalancesUiEvent) -> Unit = {},
    onNavigateToContribution: () -> Unit = {},
    onNavigateToWithdrawal: () -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current

    // Local state for the DestructiveConfirmationDialog (ephemeral, driven from sheet action)
    var contributionPendingDelete by remember { mutableStateOf<ContributionUiModel?>(null) }
    var withdrawalPendingDelete by remember { mutableStateOf<CashWithdrawalUiModel?>(null) }

    BalancesBodyContent(
        uiState = uiState,
        bottomPadding = bottomPadding,
        onEvent = onEvent,
        onNavigateToContribution = onNavigateToContribution,
        onNavigateToWithdrawal = onNavigateToWithdrawal
    )

    // ActionBottomSheet overlays (driven by UiState — survives recomposition)
    BalancesScreenOverlays(
        uiState = uiState,
        onEvent = onEvent,
        onContributionDeleteRequested = { contribution ->
            contributionPendingDelete = contribution
        },
        onWithdrawalDeleteRequested = { withdrawal ->
            withdrawalPendingDelete = withdrawal
        }
    )

    // DestructiveConfirmationDialog for contribution
    contributionPendingDelete?.let { contribution ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.balances_delete_contribution_dialog_title),
            text = stringResource(
                R.string.balances_delete_contribution_dialog_text,
                contribution.formattedAmount
            ),
            onDismiss = { contributionPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteContributionConfirmed(contribution.id))
                contributionPendingDelete = null
            }
        )
    }

    // DestructiveConfirmationDialog for cash withdrawal
    withdrawalPendingDelete?.let { withdrawal ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.balances_delete_withdrawal_dialog_title),
            text = stringResource(
                R.string.balances_delete_withdrawal_dialog_text,
                withdrawal.formattedAmount
            ),
            onDismiss = { withdrawalPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteWithdrawalConfirmed(withdrawal.id))
                withdrawalPendingDelete = null
            }
        )
    }
}
