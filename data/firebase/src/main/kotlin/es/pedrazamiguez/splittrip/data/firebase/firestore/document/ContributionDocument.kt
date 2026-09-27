package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ContributionDocument(
    val contributionId: String = "",
    val groupId: String = "",
    val userId: String = "",
    val contributionScope: String = "USER",
    val subunitId: String? = null,
    val linkedExpenseId: String? = null,
    val linkedSettlementId: String? = null,
    val amountCents: Long = 0L,
    val currency: String = "EUR",
    val equivalentBaseAmountCents: Long? = null,
    val exchangeRate: String? = null,
    val createdBy: String = "",
    var contributionDate: Timestamp? = null,
    var createdAt: Timestamp? = null,
    var lastUpdatedAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "contributions"
    }
}
