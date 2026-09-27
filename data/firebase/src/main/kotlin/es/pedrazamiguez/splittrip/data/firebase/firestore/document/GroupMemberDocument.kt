package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class GroupMemberDocument(
    val memberId: String = "",
    val groupId: String = "",
    val userId: String = "",
    val role: String = "MEMBER",
    val alias: String? = null,
    val addedBy: String = "",
    val joinedAt: Timestamp? = null
) {
    companion object {
        fun collectionPath(groupId: String) = "groups/$groupId/members"
        const val SUBCOLLECTION_PATH = "members"
        const val USER_ID_FIELD = "userId"
    }
}
