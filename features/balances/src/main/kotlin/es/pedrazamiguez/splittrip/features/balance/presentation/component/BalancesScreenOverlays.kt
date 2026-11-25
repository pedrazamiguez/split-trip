package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.export.TripReportExportBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@Composable
internal fun BalancesScreenOverlays(
    uiState: BalancesUiState,
    onEvent: (BalancesUiEvent) -> Unit,
    onNavigateToEditContribution: (String) -> Unit,
    onContributionDeleteRequested: (ContributionUiModel) -> Unit,
    onWithdrawalDeleteRequested: (CashWithdrawalUiModel) -> Unit
) {
    uiState.contributionActionsTarget?.let { contribution ->
        ActionBottomSheet(
            title = contribution.actionsTitle,
            icon = TablerIcons.Outline.Wallet,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.balances_edit_contribution_action),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {
                        onEvent(BalancesUiEvent.ContributionActionsDismissed)
                        onNavigateToEditContribution(contribution.id)
                    }
                ),
                SheetAction(
                    text = stringResource(R.string.balances_delete_contribution_action),
                    icon = TablerIcons.Outline.Trash,
                    isDestructive = true,
                    onClick = {
                        onEvent(BalancesUiEvent.ContributionActionsDismissed)
                        onContributionDeleteRequested(contribution)
                    }
                )
            ),
            onDismiss = { onEvent(BalancesUiEvent.ContributionActionsDismissed) }
        )
    }

    uiState.withdrawalToDelete?.let { withdrawal ->
        ActionBottomSheet(
            title = withdrawal.actionsTitle,
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

    if (uiState.exportSheetState.isVisible) {
        TripReportExportBottomSheet(
            sheetState = uiState.exportSheetState,
            onSelectPreset = { onEvent(BalancesUiEvent.SelectExportPeriodPreset(it)) },
            onSelectCustomStartDate = { onEvent(BalancesUiEvent.SelectExportCustomStartDate(it)) },
            onSelectCustomEndDate = { onEvent(BalancesUiEvent.SelectExportCustomEndDate(it)) },
            onSelectFormat = { onEvent(BalancesUiEvent.SelectExportFormat(it)) },
            onConfirmExport = { onEvent(BalancesUiEvent.ConfirmExportReport) },
            onSaveToDevice = { onEvent(BalancesUiEvent.SaveExportReport) },
            onDismiss = { onEvent(BalancesUiEvent.DismissExportReportSheet) }
        )
    }
}
