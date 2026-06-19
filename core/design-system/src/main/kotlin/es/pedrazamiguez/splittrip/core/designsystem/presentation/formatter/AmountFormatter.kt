package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.model.Currency as DomainCurrency
import es.pedrazamiguez.splittrip.domain.model.Expense
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Matches any Unicode "Space Separator" (category Zs):
 * U+0020 (SPACE), U+00A0 (NO-BREAK SPACE), U+202F (NARROW NO-BREAK SPACE), etc.
 */
private val UNICODE_SPACE_REGEX = Regex("[\\p{Zs}]")

fun Expense.formatAmount(locale: Locale = Locale.getDefault()): String =
    formatCurrencyAmount(amount = groupAmount, currencyCode = groupCurrency, locale = locale)

fun Expense.formatSourceAmount(locale: Locale = Locale.getDefault()): String =
    formatCurrencyAmount(amount = sourceAmount, currencyCode = sourceCurrency, locale = locale)

fun formatCurrencyAmount(amount: Long, currencyCode: String, locale: Locale): String {
    val currencyInstance =
        runCatching { Currency.getInstance(currencyCode) }.getOrElse {
            Currency.getInstance(
                AppConstants.DEFAULT_CURRENCY_CODE
            )
        }
    val fractionDigits = currencyInstance.defaultFractionDigits
    val divisor = BigDecimal.TEN.pow(fractionDigits)
    val value = BigDecimal(amount).divide(divisor, fractionDigits, RoundingMode.HALF_UP)

    val numberFormat = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currencyInstance
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }

    val formatted = numberFormat.format(value)

    // NumberFormat may render the ISO code (e.g. "CNY") instead of the native symbol
    // (e.g. "¥") when the user locale doesn't recognise the currency.
    // Fix: resolve the symbol via the currency's own locale and substitute it.
    val localeSymbol = currencyInstance.getSymbol(locale)
    val nativeSymbol = resolveNativeSymbol(currencyInstance)

    val finalFormatted = if (nativeSymbol != null &&
        localeSymbol != nativeSymbol &&
        localeSymbol == currencyInstance.currencyCode
    ) {
        formatted.replace(localeSymbol, nativeSymbol)
    } else {
        formatted
    }

    // Replace ALL Unicode space separators with non-breaking space (\u00A0)
    // to prevent the currency symbol from detaching on line breaks in Compose UI.
    //
    // NumberFormat may emit \u0020 (regular space), \u00A0 (no-break space),
    // or \u202F (narrow no-break space) depending on the Android/ICU version.
    // The regex \p{Zs} catches all of them and normalises to \u00A0, which
    // Compose Text honours as non-breaking.
    //
    // NOTE: Android notification RemoteViews does NOT respect \u00A0.
    // Notification-specific formatting is handled in NotificationAmountFormatter.
    return UNICODE_SPACE_REGEX.replace(finalFormatted, "\u00A0")
}

/**
 * Resolves the human-readable symbol for an ISO 4217 currency code.
 *
 * Tries the given [locale] first; when the JDK returns the ISO code itself
 * (e.g. "INR" for a US-locale user), falls back to [resolveNativeSymbol]
 * which finds a locale that natively uses the currency and extracts its symbol
 * (e.g. "₹").
 *
 * @param currencyCode ISO 4217 code (e.g. "INR", "EUR", "USD").
 * @param locale       The user's locale for the initial lookup.
 * @return The symbol (e.g. "₹", "€", "US$"), or an empty string if the code
 *         is blank or unresolvable.
 */
fun resolveCurrencySymbol(currencyCode: String, locale: Locale): String {
    if (currencyCode.isBlank()) return ""
    return runCatching {
        val currency = Currency.getInstance(currencyCode)
        val localeSymbol = currency.getSymbol(locale)
        if (localeSymbol == currencyCode) {
            resolveNativeSymbol(currency) ?: localeSymbol
        } else {
            localeSymbol
        }
    }.getOrDefault("")
}

/**
 * Finds the symbol for a [Currency] by looking up a locale whose country
 * actually uses that currency (its "native" locale). Returns `null` when no
 * matching locale is found or the symbol is still the ISO code.
 *
 * Disambiguates shared `$` symbols by prefixing the country code
 * (e.g., "US$", "MX$", "CA$").
 */
private fun resolveNativeSymbol(currency: Currency): String? {
    val nativeLocale = Locale.getAvailableLocales().firstOrNull { locale ->
        locale.country.isNotEmpty() &&
            runCatching {
                Currency.getInstance(locale) == currency
            }.getOrDefault(false)
    } ?: return null

    var symbol = currency.getSymbol(nativeLocale)

    // Disambiguate common shared symbols to avoid UI confusion
    if (symbol == "$") {
        symbol = when (currency.currencyCode) {
            "USD" -> "US$" // Standardises USD
            "MXN" -> "MX$" // Distinct symbol for Mexican Pesos
            "CAD" -> "CA$" // Canadian Dollars
            "AUD" -> "AU$" // Australian Dollars
            "COP" -> "CO$" // Colombian Pesos
            "CLP" -> "CL$" // Chilean Pesos
            "ARS" -> "AR$" // Argentine Pesos
            // Fallback: prepend the first two letters of the ISO code
            else -> "${currency.currencyCode.take(2)}$"
        }
    }

    return symbol.takeIf { it != currency.currencyCode }
}

fun DomainCurrency.formatDisplay(): String {
    val currencyInstance =
        runCatching { Currency.getInstance(code) }.getOrElse {
            Currency.getInstance(
                AppConstants.DEFAULT_CURRENCY_CODE
            )
        }
    var nativeSymbol = resolveNativeSymbol(currencyInstance)

    if (nativeSymbol == null || nativeSymbol == code) {
        nativeSymbol = symbol.takeIf { it.isNotBlank() && it != code }
    }

    return if (nativeSymbol?.isNotBlank() == true && nativeSymbol != code) {
        "$code ($nativeSymbol)"
    } else {
        code
    }
}

/**
 * Parses a user-entered amount string to the currency's smallest unit (e.g., cents for
 * EUR/USD, yen for JPY, millimes for TND).
 *
 * Correctly handles:
 * - Any currency's decimal places (0 for JPY, 2 for EUR/USD, 3 for KWD/TND)
 * - Locale-specific separators via [CurrencyConverter.normalizeAmountString]
 * - Deterministic rounding via [RoundingMode.HALF_UP] (no silent truncation)
 *
 * Examples:
 * - "25.50" with EUR (2 decimals) → 2550
 * - "1000"  with JPY (0 decimals) → 1000
 * - "10.500" with TND (3 decimals) → 10500
 * - "1.999" with EUR (2 decimals) → 200 (rounds, never truncates)
 *
 * @param amountString The raw user input (may use locale-specific separators)
 * @param currencyCode ISO 4217 currency code used to determine decimal places
 * @return Amount in the currency's smallest unit, or 0 if input is unparseable
 */
fun parseAmountToSmallestUnit(amountString: String, currencyCode: String): Long {
    val normalizedString = CurrencyConverter.normalizeAmountString(amountString.trim())
    val amount = normalizedString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val decimalPlaces = runCatching {
        Currency.getInstance(currencyCode).defaultFractionDigits
    }.getOrElse {
        Currency.getInstance(AppConstants.DEFAULT_CURRENCY_CODE).defaultFractionDigits
    }
    val multiplier = BigDecimal.TEN.pow(decimalPlaces)
    return amount.multiply(multiplier).setScale(0, RoundingMode.HALF_UP).toLong()
}

/**
 * Convenience function that parses a raw user-entered amount string and formats it as a
 * locale-aware currency display string in a single step.
 *
 * Combines [parseAmountToSmallestUnit] and [formatCurrencyAmount] to convert free-form
 * input (e.g. "222") into a fully formatted result (e.g. "222,00\u00A0€" for EUR with
 * Spanish locale).
 *
 * @param amountString The raw user input (may use locale-specific separators)
 * @param currencyCode ISO 4217 currency code
 * @param locale       The locale for number/currency formatting
 * @return Formatted currency string, or the original [amountString] if it is blank or
 *         [currencyCode] is blank
 */
fun formatAmountWithCurrency(amountString: String, currencyCode: String, locale: Locale): String {
    if (amountString.isBlank() || currencyCode.isBlank()) return amountString
    val cents = parseAmountToSmallestUnit(amountString, currencyCode)
    return formatCurrencyAmount(cents, currencyCode, locale)
}

/**
 * Returns `true` if the string is a valid decimal amount input, accepting both
 * dot (`.`) and comma (`,`) as decimal separator (locale-independent).
 *
 * Blank strings are considered valid — clearing the field is never an error.
 *
 * Use this for real-time `isAmountValid` checks in Event Handlers instead of
 * calling `toBigDecimalOrNull()` directly on raw user input.
 *
 * Examples:
 * - `"12.36"` → `true` (US decimal)
 * - `"12,36"` → `true` (European decimal)
 * - `"1.234,56"` → `true` (European thousands + decimal)
 * - `"1,234.56"` → `true` (US thousands + decimal)
 * - `""` or `"  "` → `true` (clearing the field)
 * - `"abc"` → `false`
 * - `"12a36"` → `false`
 */
fun String.isValidDecimalInput(): Boolean =
    isBlank() || CurrencyConverter.normalizeAmountString(trim()).toBigDecimalOrNull() != null
