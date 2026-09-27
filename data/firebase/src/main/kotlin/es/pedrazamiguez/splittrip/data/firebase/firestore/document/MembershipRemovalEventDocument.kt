package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MembershipRemovalEventDocument(
    val eventId: String = "",
    val groupId: String = "",
    val userId: String = "",
    val createdAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "membership_removal_events"
    }
}
