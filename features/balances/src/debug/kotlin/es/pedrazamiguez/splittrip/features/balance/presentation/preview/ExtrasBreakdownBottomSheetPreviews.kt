package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ExtrasBreakdownBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtraItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtrasBreakdownUiModel
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_EXTRAS_BREAKDOWN = persistentListOf(
    ExtrasBreakdownUiModel(
        typeLabel = "Fees",
        items = persistentListOf(
            ExtraItemUiModel(
                parentTitle = "ATM Bangkok",
                dateText = "15/01/2026",
                description = "Bank fee",
                formattedAmount = "3.45 €",
                scopeLabel = "Group",
                scopeType = PayerType.GROUP
            )
        ),
        formattedSubtotal = "3.45 €"
    ),
    ExtrasBreakdownUiModel(
        typeLabel = "Tips",
        items = persistentListOf(
            ExtraItemUiModel(
                parentTitle = "Dinner at Thai restaurant",
                dateText = "16/01/2026",
                description = "Service tip",
                formattedAmount = "5.00 €",
                scopeLabel = "You",
                scopeType = PayerType.USER
            )
        ),
        formattedSubtotal = "5.00 €"
    )
)

@PreviewComplete
@Composable
private fun ExtrasBreakdownBottomSheetPreview() {
    PreviewThemeWrapper {
        ExtrasBreakdownBottomSheet(
            breakdown = PREVIEW_EXTRAS_BREAKDOWN,
            formattedGrandTotal = "8.45 €",
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExtrasBreakdownBottomSheetEmptyPreview() {
    PreviewThemeWrapper {
        ExtrasBreakdownBottomSheet(
            breakdown = persistentListOf(),
            formattedGrandTotal = "0.00 €",
            onDismiss = {}
        )
    }
}
