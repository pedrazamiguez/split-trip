package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SubunitDocument(
    val subunitId: String = "",
    val groupId: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val memberShares: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    var createdAt: Timestamp? = null,
    var lastUpdatedAt: Timestamp? = null,
    @field:ServerTimestamp
    val serverUpdatedAt: Timestamp? = null,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val expireAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "subunits"
    }
}
