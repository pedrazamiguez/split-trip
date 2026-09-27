package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class GroupDocument(
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val currency: String = "EUR",
    val extraCurrencies: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val mainImagePath: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val status: String = "ACTIVE",
    val deletionRequested: Boolean = false,
    val deletedBy: String? = null,
    val deletedAt: Timestamp? = null,
    val lastArchiveEventId: String? = null
) {
    companion object {
        const val COLLECTION_PATH = "groups"
    }
}
