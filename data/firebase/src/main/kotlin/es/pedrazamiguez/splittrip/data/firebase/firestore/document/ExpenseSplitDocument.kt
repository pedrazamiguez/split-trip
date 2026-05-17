package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.PropertyName

data class ExpenseSplitDocument(
    val userId: String = "",
    val userRef: DocumentReference? = null,
    val subunitId: String? = null,
    val subunitRef: DocumentReference? = null,
    val amountCents: Long? = null,
    val percentage: String? = null,
    @get:PropertyName("excluded") @set:PropertyName("excluded")
    var isExcluded: Boolean = false,
    @get:PropertyName("coveredById") @set:PropertyName("coveredById")
    var isCoveredById: String? = null,
    @get:PropertyName("coveredByRef") @set:PropertyName("coveredByRef")
    var isCoveredByRef: DocumentReference? = null,
    val splitType: String? = null
)
