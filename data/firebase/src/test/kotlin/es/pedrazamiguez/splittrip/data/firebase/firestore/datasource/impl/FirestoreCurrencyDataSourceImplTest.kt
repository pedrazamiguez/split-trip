package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CurrenciesDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExchangeRatesDocument
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirestoreCurrencyDataSourceImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var docRef: DocumentReference
    private lateinit var snapshot: DocumentSnapshot
    private lateinit var dataSource: FirestoreCurrencyDataSourceImpl

    @BeforeEach
    fun setUp() {
        firestore = mockk()
        collectionRef = mockk()
        docRef = mockk()
        snapshot = mockk()

        every { firestore.collection("exchange_rates") } returns collectionRef

        dataSource = FirestoreCurrencyDataSourceImpl(firestore)
    }

    @Test
    fun `fetchCurrencies queries exchange_rates currencies with Source SERVER and returns mapped currencies`() =
        runTest {
            every { collectionRef.document("currencies") } returns docRef
            every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
            every { snapshot.exists() } returns true
            every { snapshot.toObject(CurrenciesDocument::class.java) } returns CurrenciesDocument(
                currencies = mapOf(
                    "USD" to "United States Dollar",
                    "EUR" to "Euro"
                )
            )

            val result = dataSource.fetchCurrencies()

            assertEquals(2, result.size)
            assertEquals("USD", result[0].code)
            assertEquals("$", result[0].symbol)
            assertEquals("EUR", result[1].code)
            assertEquals("€", result[1].symbol)
            verify(exactly = 1) { docRef.get(Source.SERVER) }
        }

    @Test
    fun `fetchCurrencies throws IllegalStateException when snapshot does not exist`() = runTest {
        every { collectionRef.document("currencies") } returns docRef
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns false

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.fetchCurrencies()
            }
        }
    }

    @Test
    fun `fetchCurrencies throws IllegalStateException when mapped currencies list is empty`() = runTest {
        every { collectionRef.document("currencies") } returns docRef
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns true
        every { snapshot.toObject(CurrenciesDocument::class.java) } returns CurrenciesDocument(
            currencies = emptyMap()
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.fetchCurrencies()
            }
        }
    }

    @Test
    fun `fetchExchangeRates queries exchange_rates latest with Source SERVER and returns mapped ExchangeRates`() =
        runTest {
            val updatedAt = Timestamp(1_795_003_200L, 0)
            every { collectionRef.document("latest") } returns docRef
            every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
            every { snapshot.exists() } returns true
            every { snapshot.toObject(ExchangeRatesDocument::class.java) } returns ExchangeRatesDocument(
                base = "USD",
                timestamp = 1_795_003_200L,
                rates = mapOf("EUR" to "0.923456"),
                updatedAt = updatedAt
            )

            val result = dataSource.fetchExchangeRates("USD")

            assertEquals("USD", result.baseCurrency.code)
            assertEquals(Instant.ofEpochSecond(1_795_003_200L), result.lastUpdated)
            assertEquals(1, result.exchangeRates.size)
            assertEquals("EUR", result.exchangeRates[0].currency.code)
            assertEquals(BigDecimal("0.923456"), result.exchangeRates[0].rate)
            verify(exactly = 1) { docRef.get(Source.SERVER) }
        }

    @Test
    fun `fetchExchangeRates throws IllegalStateException when snapshot does not exist`() = runTest {
        every { collectionRef.document("latest") } returns docRef
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns false

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.fetchExchangeRates("USD")
            }
        }
    }

    @Test
    fun `fetchExchangeRates throws IllegalStateException when mapped exchangeRates list is empty`() = runTest {
        every { collectionRef.document("latest") } returns docRef
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns true
        every { snapshot.toObject(ExchangeRatesDocument::class.java) } returns ExchangeRatesDocument(
            base = "USD",
            timestamp = 1_795_003_200L,
            rates = emptyMap()
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                dataSource.fetchExchangeRates("USD")
            }
        }
    }
}
