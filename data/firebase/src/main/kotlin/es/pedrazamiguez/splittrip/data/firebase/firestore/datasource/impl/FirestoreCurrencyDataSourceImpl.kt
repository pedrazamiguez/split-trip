package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CurrenciesDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExchangeRatesDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import kotlinx.coroutines.tasks.await

class FirestoreCurrencyDataSourceImpl(
    private val firestore: FirebaseFirestore
) : RemoteCurrencyDataSource {

    override suspend fun fetchCurrencies(): List<Currency> {
        val snapshot = firestore
            .collection(CurrenciesDocument.COLLECTION_PATH)
            .document(CurrenciesDocument.DOCUMENT_ID)
            .get(Source.SERVER)
            .await()

        check(snapshot.exists()) {
            "Firestore document ${CurrenciesDocument.COLLECTION_PATH}/${CurrenciesDocument.DOCUMENT_ID} does not exist"
        }

        val document = checkNotNull(snapshot.toObject(CurrenciesDocument::class.java)) {
            "Failed to deserialize ${CurrenciesDocument.COLLECTION_PATH}/${CurrenciesDocument.DOCUMENT_ID}"
        }

        val currencies = document.toDomain()
        check(currencies.isNotEmpty()) {
            "Firestore document ${CurrenciesDocument.COLLECTION_PATH}/" +
                "${CurrenciesDocument.DOCUMENT_ID} contained no valid currencies"
        }

        return currencies
    }

    override suspend fun fetchExchangeRates(baseCurrencyCode: String): ExchangeRates {
        val snapshot = firestore
            .collection(ExchangeRatesDocument.COLLECTION_PATH)
            .document(ExchangeRatesDocument.DOCUMENT_ID)
            .get(Source.SERVER)
            .await()

        check(snapshot.exists()) {
            "Firestore document ${ExchangeRatesDocument.COLLECTION_PATH}/" +
                "${ExchangeRatesDocument.DOCUMENT_ID} does not exist"
        }

        val document = checkNotNull(snapshot.toObject(ExchangeRatesDocument::class.java)) {
            "Failed to deserialize ${ExchangeRatesDocument.COLLECTION_PATH}/${ExchangeRatesDocument.DOCUMENT_ID}"
        }

        val exchangeRates = document.toDomain()
        check(exchangeRates.exchangeRates.isNotEmpty()) {
            "Firestore document ${ExchangeRatesDocument.COLLECTION_PATH}/" +
                "${ExchangeRatesDocument.DOCUMENT_ID} contained no valid exchange rates"
        }

        return exchangeRates
    }
}
