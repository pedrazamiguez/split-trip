package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.SettlementConsensusCard
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.ConsensusChipStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel

private val PREVIEW_CONSENSUS_PENDING = SettlementConsensusItemUiModel(
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
    confirmLabel = "I Paid",
    canDispute = true,
    status = SettlementStatus.SUGGESTED
)

private val PREVIEW_CONSENSUS_CONFIRMED = SettlementConsensusItemUiModel(
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
    confirmLabel = "I Paid",
    canDispute = false,
    status = SettlementStatus.CONFIRMED_BY_PAYER
)

private val PREVIEW_CONSENSUS_DISPUTED = SettlementConsensusItemUiModel(
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
    confirmLabel = "I Paid",
    canDispute = false,
    disputeReason = "Wrong amount entered",
    status = SettlementStatus.DISPUTED
)

private val PREVIEW_CONSENSUS_RECEIVER = SettlementConsensusItemUiModel(
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
    confirmLabel = "I Received",
    canDispute = true,
    canNudge = true,
    nudgeButtonLabel = "Remind",
    status = SettlementStatus.SUGGESTED
)

@PreviewComplete
@Composable
private fun SettlementConsensusCardPendingPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCard(
            item = PREVIEW_CONSENSUS_PENDING,
            onConfirm = {},
            onDispute = {},
            onNudge = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardConfirmedPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCard(
            item = PREVIEW_CONSENSUS_CONFIRMED,
            onConfirm = {},
            onDispute = {},
            onNudge = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardDisputedPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCard(
            item = PREVIEW_CONSENSUS_DISPUTED,
            onConfirm = {},
            onDispute = {},
            onNudge = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardSenderViewPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCard(
            item = PREVIEW_CONSENSUS_PENDING,
            onConfirm = {},
            onDispute = {},
            onNudge = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SettlementConsensusCardReceiverViewPreview() {
    PreviewThemeWrapper {
        SettlementConsensusCard(
            item = PREVIEW_CONSENSUS_RECEIVER,
            onConfirm = {},
            onDispute = {},
            onNudge = {}
        )
    }
}
