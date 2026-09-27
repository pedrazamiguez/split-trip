package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CashWithdrawalDocument(
    val withdrawalId: String = "",
    val groupId: String = "",
    val withdrawnBy: String = "",
    val withdrawalScope: String = "GROUP",
    val subunitId: String? = null,
    val amountWithdrawn: Long = 0L,
    val remainingAmount: Long = 0L,
    val currency: String = "EUR",
    val deductedBaseAmount: Long = 0L,
    val exchangeRate: String = "1",
    val addOns: List<AddOnDocument> = emptyList(),
    val title: String? = null,
    val notes: String? = null,
    val receiptLocalUri: String? = null,
    val createdBy: String = "",
    var createdAt: Timestamp? = null,
    var lastUpdatedAt: Timestamp? = null,
    val reason: String? = null
) {
    companion object {
        const val COLLECTION_PATH = "cash_withdrawals"
    }
}
