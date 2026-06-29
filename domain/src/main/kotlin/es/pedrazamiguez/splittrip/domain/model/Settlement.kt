package es.pedrazamiguez.splittrip.domain.model

/**
 * Represents a suggested peer-to-peer settlement transaction.
 *
 * @param fromUserId The ID of the member who must pay (debtor).
 * @param toUserId The ID of the member who receives the payment (creditor).
 * @param amount The settlement amount in cents (group currency).
 */
data class Settlement(
    val fromUserId: String,
    val toUserId: String,
    val amount: Long
)
