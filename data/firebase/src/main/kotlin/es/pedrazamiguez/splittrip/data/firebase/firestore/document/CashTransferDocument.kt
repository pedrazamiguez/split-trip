package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.firestore.IgnoreExtraProperties
@IgnoreExtraProperties
data class CashTransferDocument(
    val groupId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amountCents: Long = 0,
    val currency: String = "",
    val equivalentBaseAmountCents: Long = 0,
    val createdAt: Long = 0
) {
    companion object {
        const val COLLECTION_PATH = "cash_transfers"
    }
}
