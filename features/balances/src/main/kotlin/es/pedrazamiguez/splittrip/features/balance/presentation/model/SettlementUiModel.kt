package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus

data class SettlementUiModel(
    val debtorId: String,
    val creditorId: String,
    val debtorName: String,
    val creditorName: String,
    val formattedAmount: String,
    val isCurrentUserDebtor: Boolean,
    val isCurrentUserCreditor: Boolean,
    val pocketType: SettlementPocketType,
    val currencyCode: String,
    val pocketTypeLabel: String,
    val statusLabel: String,
    val statusChipStyle: PassportTagStyle,
    val status: SettlementStatus
)
