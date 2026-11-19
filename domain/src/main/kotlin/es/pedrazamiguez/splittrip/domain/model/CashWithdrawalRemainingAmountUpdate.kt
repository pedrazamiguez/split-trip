package es.pedrazamiguez.splittrip.domain.model

data class CashWithdrawalRemainingAmountUpdate(
    val withdrawalId: String,
    val remainingAmountCents: Long
)
