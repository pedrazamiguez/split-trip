package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ActivityLogDocument(
    val activityId: String = "",
    val type: String = "UNKNOWN",
    val byUserId: String = "",
    val onGroupId: String = "",
    val targetExpenseId: String? = null,
    val loggedAt: Timestamp? = null
)
