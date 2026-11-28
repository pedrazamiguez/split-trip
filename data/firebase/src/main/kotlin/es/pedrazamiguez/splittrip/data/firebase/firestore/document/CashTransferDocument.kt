package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CashTransferDocument(
    val groupId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amountCents: Long = 0,
    val currency: String = "",
    val equivalentBaseAmountCents: Long = 0,
    val createdAt: Long = 0,
    @field:ServerTimestamp
    val serverUpdatedAt: Timestamp? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val expireAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "cash_transfers"
    }
}
