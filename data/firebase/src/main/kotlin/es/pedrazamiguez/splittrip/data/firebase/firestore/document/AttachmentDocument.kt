package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class AttachmentDocument(
    val path: String = "",
    val mime: String? = null,
    val sizeBytes: Long? = null,
    val uploadedById: String? = null,
    val uploadedAt: Timestamp? = null
)
