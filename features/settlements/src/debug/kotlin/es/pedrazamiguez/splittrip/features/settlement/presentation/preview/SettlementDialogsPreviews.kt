package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.DisputeReasonDialog
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.DisputeSettlementBottomSheet
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.SettlementDisputeBanner

@PreviewComplete
@Composable
private fun DisputeReasonDialogPreview() {
    PreviewThemeWrapper {
        DisputeReasonDialog(
            reason = "I already paid in cash yesterday.",
            onReasonChanged = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DisputeSettlementBottomSheetPreview() {
    PreviewThemeWrapper {
        DisputeSettlementBottomSheet(
            reason = "The amount should be split equally.",
            onReasonChanged = {},
            onSubmit = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SettlementDisputeBannerPreview() {
    PreviewThemeWrapper {
        SettlementDisputeBanner(
            reason = "Disputed: Incorrect amount requested."
        )
    }
}
