package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.CashBreakdownBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CashBreakdownItemUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@PreviewComplete
@Composable
private fun CashBreakdownBottomSheetPreview() {
    PreviewThemeWrapper {
        CashBreakdownPreviewHelper { breakdownItems, formattedTotal ->
            CashBreakdownBottomSheet(
                memberName = "Javi",
                breakdown = breakdownItems.map {
                    CashBreakdownItemUiModel(
                        withdrawalLabel = it.withdrawalLabel,
                        dateText = it.dateText,
                        formattedRate = it.formattedRate,
                        formattedNativeRemaining = it.formattedNativeRemaining,
                        formattedEquivalent = it.formattedEquivalent,
                        scopeLabel = it.scopeLabel,
                        isEstimatedShare = it.isEstimatedShare,
                        formattedAddOns = it.formattedAddOns
                    )
                }.toImmutableList(),
                formattedTotal = formattedTotal,
                formattedTotalFees = "3.45 €",
                onDismiss = {}
            )
        }
    }
}

@PreviewComplete
@Composable
private fun CashBreakdownBottomSheetEmptyPreview() {
    PreviewThemeWrapper {
        CashBreakdownPreviewHelper(withdrawals = emptyList()) { _, _ ->
            CashBreakdownBottomSheet(
                memberName = "Andrés Pedraza Miguez",
                breakdown = persistentListOf(),
                formattedTotal = "฿ 0",
                formattedTotalFees = "",
                onDismiss = {}
            )
        }
    }
}
