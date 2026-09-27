package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SubunitDocument(
    val subunitId: String = "",
    val groupId: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val memberShares: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    var createdAt: Timestamp? = null,
    var lastUpdatedAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "subunits"
    }
}
