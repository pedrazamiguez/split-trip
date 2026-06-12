package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp

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
    val lastUpdatedAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "users"
    }
}
