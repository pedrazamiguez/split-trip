package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.SettlementConsensusCard
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.ConsensusChipStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel

internal val PREVIEW_CONSENSUS_PENDING = SettlementConsensusItemUiModel(
    settlementId = "c-1",
    counterpartyName = "María López",
    formattedAmount = "25.00 €",
    currencyCode = "EUR",
    pocketTypeLabel = "Pocket",
    directionLabel = "You owe María",
    statusLabel = "Suggested",
    statusChipStyle = ConsensusChipStyle.SUGGESTED,
    isCurrentUserPayer = true,
    canConfirm = true,
    confirmLabel = "Mark as paid",
    canDispute = true,
    status = SettlementStatus.SUGGESTED
)

internal val PREVIEW_CONSENSUS_CONFIRMED = SettlementConsensusItemUiModel(
    settlementId = "c-2",
    counterpartyName = "María López",
    formattedAmount = "25.00 €",
    currencyCode = "EUR",
    pocketTypeLabel = "Pocket",
    directionLabel = "You owe María",
    statusLabel = "Confirmed",
    statusChipStyle = ConsensusChipStyle.IN_PROGRESS,
    isCurrentUserPayer = true,
    canConfirm = false,
    confirmLabel = "Mark as paid",
    canDispute = false,
    status = SettlementStatus.CONFIRMED_BY_PAYER
)

internal val PREVIEW_CONSENSUS_DISPUTED = SettlementConsensusItemUiModel(
    settlementId = "c-3",
    counterpartyName = "María López",
    formattedAmount = "25.00 €",
    currencyCode = "EUR",
    pocketTypeLabel = "Pocket",
    directionLabel = "You owe María",
    statusLabel = "Disputed",
    statusChipStyle = ConsensusChipStyle.DISPUTED,
    isCurrentUserPayer = true,
    canConfirm = true,
    confirmLabel = "Mark as paid",
    canDispute = false,
    disputeReason = "Wrong amount entered",
    status = SettlementStatus.DISPUTED
)

internal val PREVIEW_CONSENSUS_RECEIVER = SettlementConsensusItemUiModel(
    settlementId = "c-4",
    counterpartyName = "Antonio García",
    formattedAmount = "50.00 €",
    currencyCode = "EUR",
    pocketTypeLabel = "Pocket",
    directionLabel = "Antonio owes you",
    statusLabel = "Suggested",
    statusChipStyle = ConsensusChipStyle.SUGGESTED,
    isCurrentUserPayer = false,
    canConfirm = true,
    confirmLabel = "Confirm receipt",
    canDispute = true,
    canNudge = true,
    nudgeButtonLabel = "Remind",
    status = SettlementStatus.SUGGESTED
)

@PreviewComplete
@Composable
private fun SettlementConsensusCardPendingPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCardPreviewHelper(record = PREVIEW_SETTLEMENT_RECORD_PENDING) { item ->
            SettlementConsensusCard(
                item = item,
                onConfirm = {},
                onDispute = {},
                onNudge = {}
            )
        }
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardConfirmedPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCardPreviewHelper(record = PREVIEW_SETTLEMENT_RECORD_CONFIRMED) { item ->
            SettlementConsensusCard(
                item = item,
                onConfirm = {},
                onDispute = {},
                onNudge = {}
            )
        }
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardDisputedPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCardPreviewHelper(record = PREVIEW_SETTLEMENT_RECORD_DISPUTED) { item ->
            SettlementConsensusCard(
                item = item,
                onConfirm = {},
                onDispute = {},
                onNudge = {}
            )
        }
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardSenderViewPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCardPreviewHelper(record = PREVIEW_SETTLEMENT_RECORD_PENDING) { item ->
            SettlementConsensusCard(
                item = item,
                onConfirm = {},
                onDispute = {},
                onNudge = {}
            )
        }
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardReceiverViewPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCardPreviewHelper(record = PREVIEW_SETTLEMENT_RECORD_RECEIVER) { item ->
            SettlementConsensusCard(
                item = item,
                onConfirm = {},
                onDispute = {},
                onNudge = {}
            )
        }
    }
}
