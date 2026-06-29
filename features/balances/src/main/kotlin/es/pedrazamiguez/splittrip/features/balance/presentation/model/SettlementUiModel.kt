package es.pedrazamiguez.splittrip.features.balance.presentation.model

/**
 * UI representation of a suggested settlement transaction.
 */
data class SettlementUiModel(
    val debtorId: String,
    val creditorId: String,
    val debtorName: String,
    val creditorName: String,
    val formattedAmount: String,
    val isCurrentUserDebtor: Boolean,
    val isCurrentUserCreditor: Boolean
)
