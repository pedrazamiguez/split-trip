package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.firestore.IgnoreExtraProperties
@IgnoreExtraProperties
data class SubunitMemberDocument(val subunitId: String = "", val userId: String = "")
