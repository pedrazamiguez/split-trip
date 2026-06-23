package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private object DefaultAppConfigService : AppConfigService {
    override val defaultCurrencyCode: StateFlow<String> = MutableStateFlow("EUR")
    override val balanceComputationDebounceMs: StateFlow<Long> = MutableStateFlow(300L)
}

/**
 * Shared formatting helper that wraps [LocaleProvider] and provides locale-aware
 * formatting methods for amounts, rates, cents, and percentages.
 *
 * Injected into feature mappers to eliminate copy-paste formatting wrappers.
 * Delegates to the existing top-level extension functions in `:core:design-system`.
 */
class FormattingHelper(
    private val localeProvider: LocaleProvider,
    private val appConfigService: AppConfigService = DefaultAppConfigService
) {

    /**
     * Formats an internal number string (dot decimal) to locale-aware display format.
     *
     * @param internalValue    The number in internal format (e.g., "37.22").
     * @param maxDecimalPlaces Maximum decimal places to show.
     * @param minDecimalPlaces Minimum decimal places to show (pads with zeros if needed).
     * @return The formatted string in locale format (e.g., "37,22" for Spanish).
     */
    fun formatForDisplay(internalValue: String, maxDecimalPlaces: Int, minDecimalPlaces: Int = 0): String =
        internalValue.formatNumberForDisplay(
            locale = localeProvider.getCurrentLocale(),
            maxDecimalPlaces = maxDecimalPlaces,
            minDecimalPlaces = minDecimalPlaces
        )

    /**
     * Formats an exchange rate for display using locale-aware formatting.
     *
     * @param internalValue The rate in internal format (e.g., "37.22").
     * @return Locale-formatted string (e.g., "37,22" for Spanish).
     */
    fun formatRateForDisplay(internalValue: String): String =
        internalValue.formatRateForDisplay(locale = localeProvider.getCurrentLocale())

    /**
     * Converts a raw cents value to a locale-aware, symbol-correct display string.
     *
     * @param cents        The amount in the smallest currency unit.
     * @param currencyCode ISO 4217 currency code.
     * @return Formatted currency string (e.g., "16,67\u00A0€" for Spanish locale).
     */
    fun formatCentsWithCurrency(cents: Long, currencyCode: String): String =
        formatCurrencyAmount(
            amount = cents,
            currencyCode = currencyCode,
            locale = localeProvider.getCurrentLocale(),
            defaultCurrencyCode = appConfigService.defaultCurrencyCode.value
        )

    /**
     * Formats cents to a plain decimal string for input fields.
     *
     * @param cents         The amount in the smallest currency unit.
     * @param decimalDigits Number of decimal places for the currency (default 2).
     * @return Locale-formatted plain number string (e.g., "16,67" for Spanish).
     */
    fun formatCentsValue(cents: Long, decimalDigits: Int = 2): String {
        val amount = BigDecimal(cents).movePointLeft(decimalDigits)
        return amount.toPlainString().formatNumberForDisplay(
            locale = localeProvider.getCurrentLocale(),
            maxDecimalPlaces = decimalDigits,
            minDecimalPlaces = decimalDigits
        )
    }

    /**
     * Formats a BigDecimal percentage for display (e.g., 33.33 → "33,33" in Spanish).
     *
     * @param percentage The percentage as BigDecimal.
     * @return Locale-formatted percentage string.
     */
    fun formatPercentageForDisplay(percentage: BigDecimal): String =
        percentage.toPlainString().formatNumberForDisplay(
            locale = localeProvider.getCurrentLocale(),
            maxDecimalPlaces = 2,
            minDecimalPlaces = 0
        )

    /**
     * Formats a [LocalDateTime] to a short human-readable date (e.g., "10 Jan").
     *
     * @param date The date to format. Returns an empty string if null.
     * @return Locale-aware short date string.
     */
    fun formatShortDate(date: LocalDateTime?): String =
        date?.formatShortDate(locale = localeProvider.getCurrentLocale()) ?: ""
}
