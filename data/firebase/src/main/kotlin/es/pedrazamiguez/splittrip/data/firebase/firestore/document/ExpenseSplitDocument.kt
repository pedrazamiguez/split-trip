package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class ExpenseSplitDocument(
    val userId: String = "",
    val subunitId: String? = null,
    val amountCents: Long? = null,
    val percentage: String? = null,
    @get:PropertyName("excluded") @set:PropertyName("excluded")
    var isExcluded: Boolean = false,
    @get:PropertyName("coveredById") @set:PropertyName("coveredById")
    var isCoveredById: String? = null,
    val splitType: String? = null
)
