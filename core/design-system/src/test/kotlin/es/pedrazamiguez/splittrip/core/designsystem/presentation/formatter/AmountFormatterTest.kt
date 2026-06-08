package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import es.pedrazamiguez.splittrip.domain.model.Expense
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AmountFormatter")
class AmountFormatterTest {

    private val usLocale = Locale.US
    private val esLocale = Locale.forLanguageTag("es-ES")

    // ---------- formatAmount (group currency) ----------

    @Nested
    @DisplayName("Expense.formatAmount()")
    inner class FormatAmount {

        @Test
        fun `formats EUR amount with 2 fraction digits in US locale`() {
            val expense = Expense(groupAmount = 1050, groupCurrency = "EUR")
            assertEquals("€10.50", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats EUR amount with 2 fraction digits in Spanish locale`() {
            val expense = Expense(groupAmount = 1050, groupCurrency = "EUR")
            val result = expense.formatAmount(esLocale)
            // Spanish locale: "10,50 €" — space is normalised to \u00A0 (non-breaking)
            assertEquals("10,50\u00A0€", result)
        }

        @Test
        fun `formats USD amount with 2 fraction digits`() {
            val expense = Expense(groupAmount = 50000, groupCurrency = "USD")
            assertEquals("$500.00", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats JPY amount with 0 fraction digits`() {
            val expense = Expense(groupAmount = 15725, groupCurrency = "JPY")
            assertEquals("¥15,725", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats zero amount`() {
            val expense = Expense(groupAmount = 0, groupCurrency = "EUR")
            assertEquals("€0.00", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats CNY amount with native symbol in US locale`() {
            val expense = Expense(groupAmount = 1000, groupCurrency = "CNY")
            // CNY has 2 fraction digits: 1000 -> 10.00
            // Native symbol ¥ is resolved even when user locale is US
            assertEquals("CN¥10.00", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats CNY amount with native symbol in Spanish locale`() {
            val expense = Expense(groupAmount = 1000, groupCurrency = "CNY")
            // CNY has 2 fraction digits: 1000 -> 10.00
            assertEquals("10,00\u00A0￥", expense.formatAmount(esLocale))
        }

        @Test
        fun `falls back to EUR for invalid currency code`() {
            val expense = Expense(groupAmount = 500, groupCurrency = "INVALID")
            // Fallback to EUR: 500 cents = €5.00
            assertEquals("€5.00", expense.formatAmount(usLocale))
        }
    }

    // ---------- formatSourceAmount (source currency) ----------

    @Nested
    @DisplayName("Expense.formatSourceAmount()")
    inner class FormatSourceAmount {

        @Test
        fun `formats THB source amount with 2 fraction digits in US locale`() {
            val expense = Expense(sourceAmount = 90000, sourceCurrency = "THB")
            // THB has 2 fraction digits: 90000 -> 900.00
            // Native symbol ฿ is resolved even when user locale is US
            val result = expense.formatSourceAmount(usLocale)
            assertEquals("฿900.00", result)
        }

        @Test
        fun `formats EUR source amount with 2 fraction digits`() {
            val expense = Expense(sourceAmount = 1937, sourceCurrency = "EUR")
            assertEquals("€19.37", expense.formatSourceAmount(usLocale))
        }

        @Test
        fun `formats JPY source amount with 0 fraction digits`() {
            val expense = Expense(sourceAmount = 5000, sourceCurrency = "JPY")
            assertEquals("¥5,000", expense.formatSourceAmount(usLocale))
        }

        @Test
        fun `formats source amount in Spanish locale`() {
            val expense = Expense(sourceAmount = 9000, sourceCurrency = "THB")
            val result = expense.formatSourceAmount(esLocale)
            // Spanish locale uses Baht symbol and comma as decimal separator
            // THB 9000 -> 90.00 in THB: "90,00 ฿" (with NBSP)
            assertEquals("90,00\u00A0฿", result)
        }

        @Test
        fun `formats zero source amount`() {
            val expense = Expense(sourceAmount = 0, sourceCurrency = "USD")
            assertEquals("$0.00", expense.formatSourceAmount(usLocale))
        }

        @Test
        fun `falls back to EUR for invalid source currency code`() {
            val expense = Expense(sourceAmount = 250, sourceCurrency = "NOPE")
            // Fallback to EUR: 250 cents = €2.50
            assertEquals("€2.50", expense.formatSourceAmount(usLocale))
        }
    }

    // ---------- Disambiguated Dollar Symbols ----------

    @Nested
    @DisplayName("Disambiguated Dollar Symbols")
    inner class DisambiguatedDollarSymbols {

        @Test
        fun `formats MXN with distinct symbol in Spanish locale`() {
            val expense = Expense(groupAmount = 40050, groupCurrency = "MXN")
            // The logic correctly translates "MXN" into the disambiguated "MX$"
            assertEquals("400,50\u00A0MX$", expense.formatAmount(esLocale))
        }

        @Test
        fun `formats MXN with distinct symbol in US locale`() {
            val expense = Expense(groupAmount = 40050, groupCurrency = "MXN")
            assertEquals("MX$400.50", expense.formatAmount(usLocale))
        }

        @Test
        fun `formats CAD with distinct symbol in Spanish locale`() {
            val expense = Expense(groupAmount = 15000, groupCurrency = "CAD")
            assertEquals("150,00\u00A0CA$", expense.formatAmount(esLocale))
        }

        @Test
        fun `formats fallback dollar currency (NZD) with two-letter prefix`() {
            val expense = Expense(groupAmount = 20000, groupCurrency = "NZD")
            // New Zealand Dollar is not explicitly listed, so it takes the first two letters "NZ" + "$"
            assertEquals("NZ$200.00", expense.formatAmount(usLocale))
        }
    }

    // ---------- Both formatters use same underlying logic ----------

    @Nested
    @DisplayName("Consistency between formatAmount and formatSourceAmount")
    inner class Consistency {

        @Test
        fun `same values produce identical output for same currency`() {
            val expense = Expense(
                groupAmount = 4200,
                groupCurrency = "EUR",
                sourceAmount = 4200,
                sourceCurrency = "EUR"
            )
            assertEquals(
                expense.formatAmount(usLocale),
                expense.formatSourceAmount(usLocale)
            )
        }

        @Test
        fun `different currencies produce different output`() {
            val expense = Expense(
                groupAmount = 248,
                groupCurrency = "EUR",
                sourceAmount = 9000,
                sourceCurrency = "THB"
            )
            val groupFormatted = expense.formatAmount(usLocale)
            val sourceFormatted = expense.formatSourceAmount(usLocale)
            // They should differ — EUR vs THB
            assertNotEquals(
                groupFormatted,
                sourceFormatted,
                "Expected different output but got: $groupFormatted"
            )
        }
    }

    // ---------- Non-breaking space (prevents currency symbol detachment) ----------

    @Nested
    @DisplayName("Non-breaking space in formatted output")
    inner class NonBreakingSpace {

        /**
         * Regex that matches any Unicode "Space Separator" (Zs) EXCEPT \u00A0
         * (NO-BREAK SPACE), which is the only space we allow in formatted output.
         *
         * This catches \u0020 (regular space) and \u202F (narrow no-break space)
         * which modern Android/ICU NumberFormat may emit and which are NOT
         * reliably honoured as non-breaking by Android's text layout engine.
         */
        private val breakableSpaceRegex = Regex("[\\p{Zs}&&[^\u00A0]]")

        @Test
        fun `formatted amount contains only non-breaking spaces`() {
            val currencies = listOf("EUR", "USD", "GBP", "JPY", "THB", "CNY", "MXN", "CAD")
            val locales = listOf(usLocale, esLocale, Locale.FRANCE, Locale.JAPAN)

            for (currency in currencies) {
                for (locale in locales) {
                    val result = formatCurrencyAmount(amount = 10050, currencyCode = currency, locale = locale)
                    val match = breakableSpaceRegex.find(result)
                    assertEquals(
                        null,
                        match,
                        "Breakable space U+${match?.value?.first()?.code?.toString(
                            16
                        )?.uppercase()?.padStart(4, '0')} " +
                            "found in \"$result\" for $currency / $locale"
                    )
                }
            }
        }
    }

    // ---------- isValidDecimalInput (locale-aware validation) ----------

    @Nested
    @DisplayName("String.isValidDecimalInput()")
    inner class IsValidDecimalInput {

        @Test
        fun `US dot decimal is valid`() {
            assertTrue("12.36".isValidDecimalInput())
        }

        @Test
        fun `European comma decimal is valid`() {
            assertTrue("12,36".isValidDecimalInput())
        }

        @Test
        fun `European thousands with comma decimal is valid`() {
            assertTrue("1.234,56".isValidDecimalInput())
        }

        @Test
        fun `US thousands with dot decimal is valid`() {
            assertTrue("1,234.56".isValidDecimalInput())
        }

        @Test
        fun `blank string is valid`() {
            assertTrue("".isValidDecimalInput())
        }

        @Test
        fun `whitespace-only string is valid`() {
            assertTrue("  ".isValidDecimalInput())
        }

        @Test
        fun `non-numeric string is invalid`() {
            assertFalse("abc".isValidDecimalInput())
        }

        @Test
        fun `mixed letters and digits is invalid`() {
            assertFalse("12a36".isValidDecimalInput())
        }

        @Test
        fun `double dot is valid because normalizer treats last dot as decimal`() {
            assertTrue("12..36".isValidDecimalInput())
        }

        @Test
        fun `zero is valid`() {
            assertTrue("0".isValidDecimalInput())
        }

        @Test
        fun `integer is valid`() {
            assertTrue("100".isValidDecimalInput())
        }
    }

    // ---------- Currency.formatDisplay ----------

    @Nested
    @DisplayName("Currency.formatDisplay()")
    inner class FormatDisplay {

        @Test
        fun `formats EUR with native symbol`() {
            val currency = es.pedrazamiguez.splittrip.domain.model.Currency(
                code = "EUR",
                symbol = "€",
                defaultName = "Euro",
                decimalDigits = 2
            )
            assertEquals("EUR (€)", currency.formatDisplay())
        }

        @Test
        fun `formats USD with disambiliated native symbol`() {
            val currency = es.pedrazamiguez.splittrip.domain.model.Currency(
                code = "USD",
                symbol = "$",
                defaultName = "US Dollar",
                decimalDigits = 2
            )
            assertEquals("USD (US$)", currency.formatDisplay())
        }

        @Test
        fun `formats THB falling back to model symbol`() {
            val currency = es.pedrazamiguez.splittrip.domain.model.Currency(
                code = "THB",
                symbol = "฿",
                defaultName = "Thai Baht",
                decimalDigits = 2
            )
            assertEquals("THB (฿)", currency.formatDisplay())
        }
    }
}
