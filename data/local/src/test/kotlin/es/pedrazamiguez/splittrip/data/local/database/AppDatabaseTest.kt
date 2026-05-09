package es.pedrazamiguez.splittrip.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.dao.CurrencyDao
import es.pedrazamiguez.splittrip.data.local.dao.ExchangeRateDao
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalCurrencyDataSourceImpl
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var currencyDao: CurrencyDao
    private lateinit var exchangeRateDao: ExchangeRateDao
    private lateinit var localDataSource: LocalCurrencyDataSourceImpl

    private val usd = Currency(
        "USD",
        "$",
        "US Dollar",
        2
    )
    private val eur = Currency(
        "EUR",
        "€",
        "Euro",
        2
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java
            )
            .allowMainThreadQueries() // okay for tests
            .build()
        currencyDao = db.currencyDao()
        exchangeRateDao = db.exchangeRateDao()
        localDataSource = LocalCurrencyDataSourceImpl(
            currencyDao,
            exchangeRateDao
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveAndGetCurrencies() = runTest {
        val currencies = listOf(
            usd,
            eur
        )
        localDataSource.saveCurrencies(currencies)

        val result = localDataSource.getCurrencies()
        assertEquals(
            2,
            result.size
        )
        assertEquals(
            listOf(eur.code, usd.code),
            result.map { it.code }
        )
    }

    @Test
    fun saveAndGetExchangeRates() = runTest {
        val exchangeRates = ExchangeRates(
            baseCurrency = usd,
            exchangeRates = listOf(
                ExchangeRate(
                    eur,
                    BigDecimal("0.9")
                )
            ),
            lastUpdated = Instant.now()
        )

        localDataSource.saveExchangeRates(exchangeRates)

        val result = localDataSource.getExchangeRates("USD")
        assertEquals(
            1,
            result.exchangeRates.size
        )
        assertEquals(
            eur.code,
            result.exchangeRates.first().currency.code
        )
        assertEquals(
            BigDecimal("0.9"),
            result.exchangeRates.first().rate
        )
    }

    @Test
    fun getLastUpdated() = runTest {
        val now = Instant.now()
        val exchangeRates = ExchangeRates(
            baseCurrency = usd,
            exchangeRates = listOf(
                ExchangeRate(
                    eur,
                    BigDecimal("0.9")
                )
            ),
            lastUpdated = now
        )
        localDataSource.saveExchangeRates(exchangeRates)

        val lastUpdated = localDataSource.getLastUpdated("USD")
        assertEquals(
            now.epochSecond,
            lastUpdated
        )
    }
}
