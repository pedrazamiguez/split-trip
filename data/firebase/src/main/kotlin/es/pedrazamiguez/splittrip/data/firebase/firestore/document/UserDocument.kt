package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserDocument(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String? = null,
    val profileImagePath: String? = null,
    val bio: String? = null,
    val createdBy: String = "",

    val createdAt: Timestamp? = null,
    val lastUpdatedBy: String? = null,
    val lastUpdatedAt: Timestamp? = null,
    val isPending: Boolean = false,
    val timezone: String? = null,
    val preferredReminderTime: String? = null,
    val tier: String = "FREE"
) {
    companion object {
        const val COLLECTION_PATH = "users"
    }
}
