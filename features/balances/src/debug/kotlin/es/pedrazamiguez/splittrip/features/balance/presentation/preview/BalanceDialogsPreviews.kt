package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.component.WithdrawalDeleteDialog
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel

private val PREVIEW_CONTRIBUTION = ContributionUiModel(
    id = "c-1",
    memberDisplay = MemberDisplay.Active("1", "Antonio García"),
    formattedAmount = "50.00 €"
)

private val PREVIEW_WITHDRAWAL = CashWithdrawalUiModel(
    id = "w-1",
    memberDisplay = MemberDisplay.Active("1", "Antonio García"),
    formattedAmount = "100.00 €"
)

@PreviewComplete
@Composable
private fun ContributionDeleteDialogPreview() {
    PreviewThemeWrapper {
        ContributionDeleteDialog(
            contribution = PREVIEW_CONTRIBUTION,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@PreviewComplete
@Composable
private fun WithdrawalDeleteDialogPreview() {
    PreviewThemeWrapper {
        WithdrawalDeleteDialog(
            withdrawal = PREVIEW_WITHDRAWAL,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
