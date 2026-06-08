package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@Composable
internal fun BalancesScreenOverlays(
    uiState: BalancesUiState,
    onEvent: (BalancesUiEvent) -> Unit,
    onContributionDeleteRequested: (ContributionUiModel) -> Unit,
    onWithdrawalDeleteRequested: (CashWithdrawalUiModel) -> Unit
) {
    uiState.contributionToDelete?.let { contribution ->
        ActionBottomSheet(
            title = stringResource(
                R.string.balances_contribution_actions_title,
                contribution.displayName
            ),
            icon = TablerIcons.Outline.Wallet,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.balances_delete_contribution_action),
                    icon = TablerIcons.Outline.Trash,
                    isDestructive = true,
                    onClick = {
                        onEvent(BalancesUiEvent.DeleteContributionDismissed)
                        onContributionDeleteRequested(contribution)
                    }
                )
            ),
            onDismiss = { onEvent(BalancesUiEvent.DeleteContributionDismissed) }
        )
    }

    uiState.withdrawalToDelete?.let { withdrawal ->
        ActionBottomSheet(
            title = stringResource(
                R.string.balances_withdrawal_actions_title,
                withdrawal.displayName
            ),
            icon = TablerIcons.Outline.CashBanknote,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.balances_delete_withdrawal_action),
                    icon = TablerIcons.Outline.Trash,
                    isDestructive = true,
                    onClick = {
                        onEvent(BalancesUiEvent.DeleteWithdrawalDismissed)
                        onWithdrawalDeleteRequested(withdrawal)
                    }
                )
            ),
            onDismiss = { onEvent(BalancesUiEvent.DeleteWithdrawalDismissed) }
        )
    }
}
