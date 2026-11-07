package es.pedrazamiguez.splittrip.domain.converter

import es.pedrazamiguez.splittrip.domain.exception.ValidationException
import es.pedrazamiguez.splittrip.domain.model.Currency
import es.pedrazamiguez.splittrip.domain.model.ExchangeRate
import es.pedrazamiguez.splittrip.domain.model.ExchangeRates
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CurrencyConverterTest {

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
    private val mxn = Currency(
        "MXN",
        "$",
        "Mexican Peso",
        2
    )

    private val rates = ExchangeRates(
        baseCurrency = usd,
        exchangeRates = listOf(
            ExchangeRate(
                eur,
                BigDecimal("0.9")
            ), // 1 USD =  0.9 EUR
            ExchangeRate(
                mxn,
                BigDecimal("20.0")
            ) // 1 USD = 20.0 MXN
        ),
        lastUpdated = Instant.now()
    )

    @Test
    fun `convert USD to EUR`() = runTest {
        val result = CurrencyConverter.convert(
            BigDecimal("10.00"),
            usd,
            eur,
            rates
        )
        assertEquals(
            BigDecimal("9.00"),
            result
        )
    }

    @Test
    fun `convert EUR to USD`() = runTest {
        val result = CurrencyConverter.convert(
            BigDecimal("9.00"),
            eur,
            usd,
            rates
        )
        assertEquals(
            BigDecimal("10.00"),
            result
        )
    }

    @Test
    fun `convert EUR to MXN`() = runTest {
        val result = CurrencyConverter.convert(
            BigDecimal("9.00"),
            eur,
            mxn,
            rates
        )
        assertEquals(
            BigDecimal("200.00"),
            result
        )
    }

    @Test
    fun `throws if missing rate`() { // No runTest needed for assertThrows
        val inr = Currency(
            "INR",
            "₹",
            "Indian Rupee",
            2
        )
        assertThrows<IllegalArgumentException> {
            CurrencyConverter.convert(
                BigDecimal("10.00"),
                inr,
                eur,
                rates
            )
        }
    }

    @Nested
    inner class ParseToCentsSharedTests {

        @Test
        fun `parseToCents with empty string fails`() {
            val result = CurrencyConverter.parseToCents("")
            val exception = result.exceptionOrNull()
            assert(exception is ValidationException)
            assertEquals(
                "Amount cannot be empty",
                exception?.message
            )
        }

        @Test
        fun `parseToCents with blank string fails`() {
            val result = CurrencyConverter.parseToCents("   ")
            val exception = result.exceptionOrNull()
            assert(exception is ValidationException)
            assertEquals(
                "Amount cannot be empty",
                exception?.message
            )
        }

        @Test
        fun `parseToCents with zero fails`() {
            val result = CurrencyConverter.parseToCents("0")
            val exception = result.exceptionOrNull()
            assert(exception is ValidationException)
            assertEquals(
                "Amount must be greater than zero",
                exception?.message
            )
        }

        @Test
        fun `parseToCents with negative number fails`() {
            val result = CurrencyConverter.parseToCents("-10.50")
            val exception = result.exceptionOrNull()
            assert(exception is ValidationException)
            assertEquals(
                "Amount must be greater than zero",
                exception?.message
            )
        }

        @Test
        fun `parseToCents with invalid characters fails`() {
            val result = CurrencyConverter.parseToCents("abc")
            val exception = result.exceptionOrNull()
            assert(exception is ValidationException)
            assertEquals(
                "Please enter a valid amount",
                exception?.message
            )
        }

        @Test
        fun `parseToCents with mixed separators handles US format`() {
            // "1,200.50" is treated as US format: comma as thousand separator, dot as decimal
            // Last separator (.) is treated as decimal, comma is removed
            val result = CurrencyConverter.parseToCents("1,200.50")
            assert(result.isSuccess)
            assertEquals(120050L, result.getOrNull()) // 1200.50 = 120050 cents
        }
    }

    @Nested
    inner class ParseToCentsUsLocale {
        private val originalLocale = Locale.getDefault()

        @BeforeEach
        fun setup() {
            // Force the locale to US (where '.' is the decimal)
            Locale.setDefault(Locale.US)
        }

        @AfterEach
        fun teardown() {
            // Restore the original locale
            Locale.setDefault(originalLocale)
        }

        @Test
        fun `parses integer string`() {
            assertEquals(
                1200L,
                CurrencyConverter
                    .parseToCents("12")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses string with dot decimal`() {
            assertEquals(
                1256L,
                CurrencyConverter
                    .parseToCents("12.56")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses string with comma decimal (fallback)`() {
            // US locale primary is '.', so it tries ',' as a fallback
            assertEquals(
                1256L,
                CurrencyConverter
                    .parseToCents("12,56")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses and rounds up correctly`() {
            assertEquals(
                1257L,
                CurrencyConverter
                    .parseToCents("12.567")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses and rounds half-up correctly`() {
            assertEquals(
                1257L,
                CurrencyConverter
                    .parseToCents("12.565")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses and rounds down correctly`() {
            assertEquals(
                1256L,
                CurrencyConverter
                    .parseToCents("12.562")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses example from prompt`() {
            assertEquals(
                142716L,
                CurrencyConverter
                    .parseToCents("1427.158")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses with whitespace`() {
            assertEquals(
                550L,
                CurrencyConverter
                    .parseToCents(" 5.50 ")
                    .getOrThrow()
            )
        }
    }

    @Nested
    inner class ParseToCentsGermanLocale {
        private val originalLocale = Locale.getDefault()

        @BeforeEach
        fun setup() {
            // Force the locale to German (where ',' is the decimal)
            Locale.setDefault(Locale.GERMAN)
        }

        @AfterEach
        fun teardown() {
            // Restore the original locale
            Locale.setDefault(originalLocale)
        }

        @Test
        fun `parses integer string`() {
            assertEquals(
                1200L,
                CurrencyConverter
                    .parseToCents("12")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses string with comma decimal`() {
            assertEquals(
                1256L,
                CurrencyConverter
                    .parseToCents("12,56")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses string with dot decimal (fallback)`() {
            // German locale primary is ',', so it tries '.' as a fallback
            assertEquals(
                1256L,
                CurrencyConverter
                    .parseToCents("12.56")
                    .getOrThrow()
            )
        }

        @Test
        fun `parses and rounds up correctly`() {
            assertEquals(
                1257L,
                CurrencyConverter
                    .parseToCents("12,567")
                    .getOrThrow()
            )
        }
    }

    @Nested
    inner class NormalizeAmountString {

        @Test
        fun `returns 0 for empty string`() {
            assertEquals("0", CurrencyConverter.normalizeAmountString(""))
        }

        @Test
        fun `returns 0 for blank string`() {
            assertEquals("0", CurrencyConverter.normalizeAmountString("   "))
        }

        @Test
        fun `returns input unchanged when no separators`() {
            assertEquals("12345", CurrencyConverter.normalizeAmountString("12345"))
        }

        @Test
        fun `handles US format with dot decimal`() {
            assertEquals("1245.56", CurrencyConverter.normalizeAmountString("1245.56"))
        }

        @Test
        fun `handles European format with comma decimal`() {
            assertEquals("1245.56", CurrencyConverter.normalizeAmountString("1245,56"))
        }

        @Test
        fun `handles US format with thousand separator comma`() {
            assertEquals("1245.56", CurrencyConverter.normalizeAmountString("1,245.56"))
        }

        @Test
        fun `handles European format with thousand separator dot`() {
            assertEquals("1245.56", CurrencyConverter.normalizeAmountString("1.245,56"))
        }

        @Test
        fun `handles multiple thousand separators US format`() {
            assertEquals("1234567.89", CurrencyConverter.normalizeAmountString("1,234,567.89"))
        }

        @Test
        fun `handles multiple thousand separators European format`() {
            assertEquals("1234567.89", CurrencyConverter.normalizeAmountString("1.234.567,89"))
        }

        @Test
        fun `handles single separator as decimal - dot`() {
            assertEquals("12.34", CurrencyConverter.normalizeAmountString("12.34"))
        }

        @Test
        fun `handles single separator as decimal - comma`() {
            assertEquals("12.34", CurrencyConverter.normalizeAmountString("12,34"))
        }

        @Test
        fun `handles ambiguous case with single comma (treats as decimal)`() {
            // "6,666" - only one separator, treated as decimal
            assertEquals("6.666", CurrencyConverter.normalizeAmountString("6,666"))
        }

        @Test
        fun `handles ambiguous case with single dot (treats as decimal)`() {
            // "6.666" - only one separator, treated as decimal
            assertEquals("6.666", CurrencyConverter.normalizeAmountString("6.666"))
        }

        @Test
        fun `handles integer with trailing decimal separator - dot`() {
            assertEquals("100.", CurrencyConverter.normalizeAmountString("100."))
        }

        @Test
        fun `handles integer with trailing decimal separator - comma`() {
            assertEquals("100.", CurrencyConverter.normalizeAmountString("100,"))
        }

        @Test
        fun `handles decimal starting with separator - dot`() {
            assertEquals(".99", CurrencyConverter.normalizeAmountString(".99"))
        }

        @Test
        fun `handles decimal starting with separator - comma`() {
            assertEquals(".99", CurrencyConverter.normalizeAmountString(",99"))
        }

        @Test
        fun `handles large number with mixed separators`() {
            assertEquals("123456789.12", CurrencyConverter.normalizeAmountString("123,456,789.12"))
        }
    }

    @Nested
    inner class IsPositiveAmountTests {

        @Test
        fun `isPositiveAmount returns true for valid positive amounts`() {
            assertTrue(CurrencyConverter.isPositiveAmount("10.50"))
            assertTrue(CurrencyConverter.isPositiveAmount("1"))
            assertTrue(CurrencyConverter.isPositiveAmount("1,234.56"))
            assertTrue(CurrencyConverter.isPositiveAmount("1.234,56"))
        }

        @Test
        fun `isPositiveAmount returns false for zero amounts`() {
            assertFalse(CurrencyConverter.isPositiveAmount("0"))
            assertFalse(CurrencyConverter.isPositiveAmount("0.0"))
            assertFalse(CurrencyConverter.isPositiveAmount("0.00"))
        }

        @Test
        fun `isPositiveAmount returns false for negative amounts`() {
            assertFalse(CurrencyConverter.isPositiveAmount("-0.94"))
            assertFalse(CurrencyConverter.isPositiveAmount("-1"))
            assertFalse(CurrencyConverter.isPositiveAmount("-1.234,56"))
        }

        @Test
        fun `isPositiveAmount returns false for empty or blank strings`() {
            assertFalse(CurrencyConverter.isPositiveAmount(""))
            assertFalse(CurrencyConverter.isPositiveAmount("   "))
        }

        @Test
        fun `isPositiveAmount returns false for invalid strings`() {
            assertFalse(CurrencyConverter.isPositiveAmount("abc"))
            assertFalse(CurrencyConverter.isPositiveAmount("12a34"))
        }
    }
}
