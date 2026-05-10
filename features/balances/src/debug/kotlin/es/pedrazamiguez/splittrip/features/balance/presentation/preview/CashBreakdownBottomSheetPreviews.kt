package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.balance.presentation.component.CashBreakdownBottomSheet
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun CashBreakdownBottomSheetPreview() {
    CashBreakdownPreviewHelper { breakdown, formattedTotal ->
        CashBreakdownBottomSheet(
            breakdown = breakdown,
            formattedTotal = formattedTotal,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun CashBreakdownBottomSheetEmptyPreview() {
    CashBreakdownPreviewHelper(withdrawals = emptyList()) { _, _ ->
        CashBreakdownBottomSheet(
            breakdown = persistentListOf(),
            formattedTotal = "฿ 0",
            onDismiss = {}
        )
    }
}
