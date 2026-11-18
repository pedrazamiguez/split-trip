package es.pedrazamiguez.splittrip.data.firebase.firestore.document

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CurrenciesDocument(
    val currencies: Map<String, String> = emptyMap(),
    val updatedAt: Timestamp? = null
) {
    companion object {
        const val COLLECTION_PATH = "exchange_rates"
        const val DOCUMENT_ID = "currencies"
    }
}
