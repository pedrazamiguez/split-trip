package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel

@Composable
internal fun ContributionDeleteDialog(
    contribution: ContributionUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DestructiveConfirmationDialog(
        title = stringResource(R.string.balances_delete_contribution_dialog_title),
        text = stringResource(
            R.string.balances_delete_contribution_dialog_text,
            contribution.formattedAmount
        ),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
