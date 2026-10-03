package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CurrencyPickerUtils")
class CurrencyPickerUtilsTest {

    @Nested
    @DisplayName("resolveCurrencyItemHeadline()")
    inner class ResolveCurrencyItemHeadline {

        @Test
        @DisplayName("prefers localized name when available")
        fun resolveCurrencyItemHeadline_withLocalizedName_returnsLocalizedName() {
            val currency = CurrencyUiModel(
                code = "GBP",
                displayText = "GBP (£)",
                decimalDigits = 2,
                defaultName = "British Pound Sterling",
                localizedName = "Libra esterlina"
            )

            val result = resolveCurrencyItemHeadline(currency)

            assertEquals("Libra esterlina", result)
        }

        @Test
        @DisplayName("falls back to default name when localized name is blank")
        fun resolveCurrencyItemHeadline_withBlankLocalizedName_returnsDefaultName() {
            val currency = CurrencyUiModel(
                code = "CHF",
                displayText = "CHF",
                decimalDigits = 2,
                defaultName = "Swiss Franc",
                localizedName = ""
            )

            val result = resolveCurrencyItemHeadline(currency)

            assertEquals("Swiss Franc", result)
        }

        @Test
        @DisplayName("falls back to currency code when both localized and default names are blank")
        fun resolveCurrencyItemHeadline_withBlankNames_returnsCode() {
            val currency = CurrencyUiModel(
                code = "XYZ",
                displayText = "XYZ",
                decimalDigits = 2,
                defaultName = "",
                localizedName = ""
            )

            val result = resolveCurrencyItemHeadline(currency)

            assertEquals("XYZ", result)
        }
    }

    @Nested
    @DisplayName("filterCurrencies()")
    inner class FilterCurrencies {

        private val currencies = listOf(
            CurrencyUiModel("EUR", "EUR (€)", 2, "Euro", "Euro"),
            CurrencyUiModel("USD", "USD ($)", 2, "US Dollar", "Dólar estadounidense"),
            CurrencyUiModel("JPY", "JPY (¥)", 0, "Japanese Yen", "Yen japonés")
        ).toImmutableList()

        @Test
        @DisplayName("returns all currencies when query is blank")
        fun returnsAllCurrenciesWhenQueryIsBlank() {
            val result = filterCurrencies(currencies, "   ")
            assertEquals(currencies, result)
        }

        @Test
        @DisplayName("matches by currency code case-insensitively")
        fun matchesByCurrencyCodeCaseInsensitively() {
            val result = filterCurrencies(currencies, "eur")
            assertEquals(listOf(currencies[0]), result)
        }

        @Test
        @DisplayName("matches by localized name case-insensitively")
        fun matchesByLocalizedNameCaseInsensitively() {
            val result = filterCurrencies(currencies, "dólar")
            assertEquals(listOf(currencies[1]), result)
        }

        @Test
        @DisplayName("matches by default name case-insensitively")
        fun matchesByDefaultNameCaseInsensitively() {
            val result = filterCurrencies(currencies, "yen")
            assertEquals(listOf(currencies[2]), result)
        }

        @Test
        @DisplayName("returns empty list when no currency matches")
        fun returnsEmptyListWhenNoMatches() {
            val result = filterCurrencies(currencies, "nonexistent")
            assertEquals(emptyList<CurrencyUiModel>(), result)
        }
    }

    @Nested
    @DisplayName("Constants")
    inner class Constants {

        @Test
        @DisplayName("currencyPickerSearchThreshold is six")
        fun currencyPickerSearchThreshold_isSix() {
            assertEquals(6, CURRENCY_PICKER_SEARCH_THRESHOLD)
        }
    }
}
