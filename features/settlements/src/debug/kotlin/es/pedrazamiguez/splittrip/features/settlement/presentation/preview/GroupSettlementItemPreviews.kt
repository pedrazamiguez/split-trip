package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.GroupSettlementItem
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowStatusStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowUiModel

private val PREVIEW_ROW_PENDING = SettlementRowUiModel(
    settlementId = "r-1",
    debtorId = "user-1",
    creditorId = "user-2",
    debtorName = "Antonio",
    creditorName = "María",
    directionTitle = "Antonio owes María",
    formattedAmount = "25.00 €",
    isCurrentUserDebtor = true,
    isCurrentUserCreditor = false,
    pocketTypeLabel = "Pocket",
    currencyCode = "EUR",
    statusLabel = "Pending",
    statusChipStyle = SettlementRowStatusStyle.WARNING,
    canCurrentUserConfirm = true,
    canCurrentUserDispute = true,
    disputedByCurrentUser = false,
    status = SettlementStatus.SUGGESTED
)

private val PREVIEW_ROW_SETTLED = SettlementRowUiModel(
    settlementId = "r-2",
    debtorId = "user-1",
    creditorId = "user-2",
    debtorName = "Antonio",
    creditorName = "María",
    directionTitle = "Antonio paid María",
    formattedAmount = "25.00 €",
    isCurrentUserDebtor = true,
    isCurrentUserCreditor = false,
    pocketTypeLabel = "Pocket",
    currencyCode = "EUR",
    statusLabel = "Settled",
    statusChipStyle = SettlementRowStatusStyle.SUCCESS,
    canCurrentUserConfirm = false,
    canCurrentUserDispute = false,
    disputedByCurrentUser = false,
    status = SettlementStatus.RESOLVED
)

private val PREVIEW_ROW_DISPUTED = SettlementRowUiModel(
    settlementId = "r-3",
    debtorId = "user-1",
    creditorId = "user-2",
    debtorName = "Antonio",
    creditorName = "María",
    directionTitle = "Antonio owes María",
    formattedAmount = "25.00 €",
    isCurrentUserDebtor = true,
    isCurrentUserCreditor = false,
    pocketTypeLabel = "Pocket",
    currencyCode = "EUR",
    statusLabel = "Disputed",
    statusChipStyle = SettlementRowStatusStyle.ERROR,
    canCurrentUserConfirm = true,
    canCurrentUserDispute = false,
    disputedByCurrentUser = false,
    disputeReason = "Payment not received",
    status = SettlementStatus.DISPUTED
)

@PreviewComplete
@Composable
private fun GroupSettlementItemPendingPreview() {
    PreviewThemeWrapper {
        GroupSettlementItem(
            settlement = PREVIEW_ROW_PENDING,
            onConfirm = {},
            onDispute = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementItemSettledPreview() {
    PreviewThemeWrapper {
        GroupSettlementItem(
            settlement = PREVIEW_ROW_SETTLED,
            onConfirm = {},
            onDispute = {}
        )
    }
}

@PreviewComplete
@Composable
private fun GroupSettlementItemDisputedPreview() {
    PreviewThemeWrapper {
        GroupSettlementItem(
            settlement = PREVIEW_ROW_DISPUTED,
            onConfirm = {},
            onDispute = {}
        )
    }
}
