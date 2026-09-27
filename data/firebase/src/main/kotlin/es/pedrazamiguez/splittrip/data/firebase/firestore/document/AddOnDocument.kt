package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.firestore.IgnoreExtraProperties
/**
 * Firestore document representation of a structured add-on.
 *
 * Exchange rate is stored as [String] (via [java.math.BigDecimal.toPlainString])
 * to avoid IEEE 754 floating-point precision loss in Firestore's number serialization.
 */
@IgnoreExtraProperties
data class AddOnDocument(
    val id: String = "",
    val type: String = "FEE",
    val mode: String = "ON_TOP",
    val valueType: String = "EXACT",
    val amountCents: Long = 0L,
    val currency: String = "EUR",
    val exchangeRate: String? = null,
    val groupAmountCents: Long = 0L,
    val paymentMethod: String = "OTHER",
    val description: String? = null
)
