package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.Timestamp
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CurrenciesDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExchangeRatesDocument
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CurrencyDocumentMapperTest {

    @Test
    fun `CurrenciesDocument toDomain maps known symbol USD to dollar sign and 2 decimal digits`() {
        val document = CurrenciesDocument(currencies = mapOf("USD" to "United States Dollar"))

        val result = document.toDomain()

        assertEquals(1, result.size)
        assertEquals("USD", result[0].code)
        assertEquals("$", result[0].symbol)
        assertEquals("United States Dollar", result[0].defaultName)
        assertEquals(2, result[0].decimalDigits)
    }

    @Test
    fun `CurrenciesDocument toDomain maps known symbol EUR to euro sign`() {
        val document = CurrenciesDocument(currencies = mapOf("EUR" to "Euro"))

        val result = document.toDomain()

        assertEquals(1, result.size)
        assertEquals("EUR", result[0].code)
        assertEquals("€", result[0].symbol)
        assertEquals("Euro", result[0].defaultName)
        assertEquals(2, result[0].decimalDigits)
    }

    @Test
    fun `CurrenciesDocument toDomain falls back to currency code as symbol for unknown currency code`() {
        val document = CurrenciesDocument(currencies = mapOf("XYZ" to "Unknown Currency"))

        val result = document.toDomain()

        assertEquals(1, result.size)
        assertEquals("XYZ", result[0].code)
        assertEquals("XYZ", result[0].symbol)
        assertEquals("Unknown Currency", result[0].defaultName)
        assertEquals(2, result[0].decimalDigits)
    }

    @Test
    fun `CurrenciesDocument toDomain maps zero-decimal currencies (JPY, KRW, VND, CLP, ISK) to 0 decimal digits`() {
        val zeroDecimalCodes = listOf("JPY", "KRW", "VND", "CLP", "ISK")
        val document = CurrenciesDocument(
            currencies = zeroDecimalCodes.associateWith { "$it Name" }
        )

        val result = document.toDomain()

        assertEquals(5, result.size)
        result.forEach { currency ->
            assertEquals(0, currency.decimalDigits, "Expected 0 decimal digits for ${currency.code}")
        }
    }

    @Test
    fun `CurrenciesDocument toDomain maps three-decimal currencies (BHD, KWD, OMR, TND) to 3 decimal digits`() {
        val threeDecimalCodes = listOf("BHD", "KWD", "OMR", "TND")
        val document = CurrenciesDocument(
            currencies = threeDecimalCodes.associateWith { "$it Name" }
        )

        val result = document.toDomain()

        assertEquals(4, result.size)
        result.forEach { currency ->
            assertEquals(3, currency.decimalDigits, "Expected 3 decimal digits for ${currency.code}")
        }
    }

    @Test
    fun `CurrenciesDocument toDomain filters out invalid non-ISO currency codes`() {
        val document = CurrenciesDocument(
            currencies = mapOf(
                "USD" to "United States Dollar",
                "usd" to "Lowercase",
                "US" to "Too short",
                "USDT" to "Too long",
                "123" to "Numeric"
            )
        )

        val result = document.toDomain()

        assertEquals(1, result.size)
        assertEquals("USD", result[0].code)
    }

    @Test
    fun `CurrenciesDocument toDomain returns empty list when currencies map is empty`() {
        val document = CurrenciesDocument(currencies = emptyMap())

        val result = document.toDomain()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `ExchangeRatesDocument toDomain maps base currency, string rates, and updatedAt Timestamp to Instant`() {
        val updatedAt = Timestamp(1_795_003_200L, 0)
        val document = ExchangeRatesDocument(
            base = "USD",
            timestamp = 1_700_000_000L,
            rates = mapOf(
                "EUR" to "0.923456",
                "JPY" to "154.12",
                "KWD" to "0.307"
            ),
            updatedAt = updatedAt
        )

        val result = document.toDomain()

        assertEquals("USD", result.baseCurrency.code)
        assertEquals("$", result.baseCurrency.symbol)
        assertEquals(2, result.baseCurrency.decimalDigits)
        assertEquals(Instant.ofEpochSecond(1_795_003_200L), result.lastUpdated)
        assertEquals(3, result.exchangeRates.size)

        val eurRate = result.exchangeRates.first { it.currency.code == "EUR" }
        assertEquals("€", eurRate.currency.symbol)
        assertEquals(2, eurRate.currency.decimalDigits)
        assertEquals(BigDecimal("0.923456"), eurRate.rate)

        val jpyRate = result.exchangeRates.first { it.currency.code == "JPY" }
        assertEquals("¥", jpyRate.currency.symbol)
        assertEquals(0, jpyRate.currency.decimalDigits)
        assertEquals(BigDecimal("154.12"), jpyRate.rate)

        val kwdRate = result.exchangeRates.first { it.currency.code == "KWD" }
        assertEquals("د.ك", kwdRate.currency.symbol)
        assertEquals(3, kwdRate.currency.decimalDigits)
        assertEquals(BigDecimal("0.307"), kwdRate.rate)
    }

    @Test
    fun `ExchangeRatesDocument toDomain falls back to timestamp epoch seconds when updatedAt is null`() {
        val document = ExchangeRatesDocument(
            base = "USD",
            timestamp = 1_795_000_000L,
            rates = mapOf("EUR" to "0.92"),
            updatedAt = null
        )

        val result = document.toDomain()

        assertEquals(Instant.ofEpochSecond(1_795_000_000L), result.lastUpdated)
    }

    @Test
    fun `ExchangeRatesDocument toDomain falls back to Instant EPOCH when both updatedAt and timestamp are absent`() {
        val document = ExchangeRatesDocument(
            base = "USD",
            timestamp = 0L,
            rates = mapOf("EUR" to "0.92"),
            updatedAt = null
        )

        val result = document.toDomain()

        assertEquals(Instant.EPOCH, result.lastUpdated)
    }

    @Test
    fun `ExchangeRatesDocument toDomain skips unparseable or negative rates and invalid currency codes`() {
        val document = ExchangeRatesDocument(
            base = "USD",
            timestamp = 1_795_000_000L,
            rates = mapOf(
                "EUR" to "0.92",
                "GBP" to "not-a-number",
                "CAD" to "-1.25",
                "invalid" to "1.10"
            )
        )

        val result = document.toDomain()

        assertEquals(1, result.exchangeRates.size)
        assertEquals("EUR", result.exchangeRates[0].currency.code)
        assertEquals(BigDecimal("0.92"), result.exchangeRates[0].rate)
    }

    @Test
    fun `ExchangeRatesDocument toDomain falls back to USD when base currency code is invalid`() {
        val document = ExchangeRatesDocument(
            base = "invalid",
            timestamp = 1_795_000_000L,
            rates = mapOf("EUR" to "0.92")
        )

        val result = document.toDomain()

        assertEquals("USD", result.baseCurrency.code)
        assertEquals("$", result.baseCurrency.symbol)
    }
}
