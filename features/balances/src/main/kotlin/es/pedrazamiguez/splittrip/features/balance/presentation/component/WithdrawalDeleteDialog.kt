package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel

@Composable
internal fun WithdrawalDeleteDialog(
    withdrawal: CashWithdrawalUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DestructiveConfirmationDialog(
        title = stringResource(R.string.balances_delete_withdrawal_dialog_title),
        text = stringResource(
            R.string.balances_delete_withdrawal_dialog_text,
            withdrawal.formattedAmount
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
