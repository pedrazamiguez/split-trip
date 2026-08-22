package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp

/**
 * Firestore document representation of a payment tranche / sub-expense.
 *
 * Exchange rate is stored as [String] (via [java.math.BigDecimal.toPlainString])
 * to avoid IEEE 754 floating-point precision loss.
 */
data class SubExpenseDocument(
    val id: String = "",
    val title: String = "",
    val amountCents: Long = 0L,
    val currency: String = "EUR",
    val groupAmountCents: Long = 0L,
    val exchangeRate: String? = null,
    val paymentMethod: String = "OTHER",
    val paymentStatus: String = "FINISHED",
    val payerType: String = "GROUP",
    val payerId: String? = null,
    val dueDate: Timestamp? = null,
    val operationDate: Timestamp? = null,
    val notes: String? = null,
    val addOns: List<AddOnDocument> = emptyList(),
    val cashTranches: List<Map<String, Any>> = emptyList()
)
