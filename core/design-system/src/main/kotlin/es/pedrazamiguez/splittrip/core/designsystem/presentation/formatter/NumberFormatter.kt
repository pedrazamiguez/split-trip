package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionResult
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_RATE_DECIMALS = 6

private val decimalFormatSymbolsCache = ConcurrentHashMap<Locale, DecimalFormatSymbols>()
private val decimalFormatCache = ConcurrentHashMap<Pair<String, Locale>, DecimalFormat>()

internal fun getDecimalFormatCacheSize(): Int = decimalFormatCache.size
internal fun clearNumberFormatterCache() {
    decimalFormatSymbolsCache.clear()
    decimalFormatCache.clear()
}

private fun getDecimalFormatSymbols(locale: Locale): DecimalFormatSymbols =
    decimalFormatSymbolsCache.computeIfAbsent(locale) { DecimalFormatSymbols.getInstance(it) }

private fun getDecimalFormat(pattern: String, locale: Locale): DecimalFormat {
    val prototype = decimalFormatCache.computeIfAbsent(pattern to locale) { (p, l) ->
        DecimalFormat(p, getDecimalFormatSymbols(l))
    }
    return prototype.clone() as DecimalFormat
}

/**
 * Formats a number string from internal format (dot decimal) to locale-aware display format.
 *
 * @param locale The locale to use for formatting (default: device locale)
 * @param maxDecimalPlaces Maximum decimal places to show
 * @param minDecimalPlaces Minimum decimal places to show (pads with zeros if needed)
 * @return The formatted string in locale format (e.g., "37,22" for Spanish)
 */
fun String.formatNumberForDisplay(
    locale: Locale,
    maxDecimalPlaces: Int = DEFAULT_RATE_DECIMALS,
    minDecimalPlaces: Int = 0
): String {
    val number = this.toBigDecimalOrNull() ?: return this

    val pattern = buildNumberPattern(maxDecimalPlaces, minDecimalPlaces)
    val formatter = getDecimalFormat(pattern, locale)

    return formatter.format(number)
}

/**
 * Formats an exchange rate for display using locale-aware formatting.
 *
 * @param locale The locale to use for formatting (default: device locale)
 * @return Locale-formatted string (e.g., "37,22" for Spanish)
 */
fun String.formatRateForDisplay(locale: Locale): String =
    this.formatNumberForDisplay(locale, DEFAULT_RATE_DECIMALS, minDecimalPlaces = 0)

/**
 * Formats a BigDecimal number to locale-aware display format.
 *
 * @param locale The locale to use for formatting (default: device locale)
 * @param maxDecimalPlaces Maximum decimal places to show
 * @param minDecimalPlaces Minimum decimal places to show (pads with zeros if needed)
 * @return The formatted string in locale format
 */
fun BigDecimal.formatForDisplay(
    locale: Locale,
    maxDecimalPlaces: Int = 2,
    minDecimalPlaces: Int = 0
): String {
    val pattern = buildNumberPattern(maxDecimalPlaces, minDecimalPlaces)
    val formatter = getDecimalFormat(pattern, locale)

    return formatter.format(this)
}

private fun buildNumberPattern(maxDecimalPlaces: Int, minDecimalPlaces: Int = 0): String = if (maxDecimalPlaces > 0) {
    val requiredDecimals = minDecimalPlaces.coerceIn(0, maxDecimalPlaces)
    val optionalDecimals = maxDecimalPlaces - requiredDecimals
    val decimalPart = "0".repeat(requiredDecimals) + "#".repeat(optionalDecimals)
    "#,##0.$decimalPart"
} else {
    "#,##0"
}

/**
 * Formats the live preview text shown in the arithmetic operator bar.
 *
 * Returns a formatted string (e.g., "= 170,20") if there is a successful evaluation result
 * and the expression contains operators. Otherwise returns an empty string.
 */
fun formatArithmeticPreview(
    expressionBuffer: String,
    evaluationResult: ExpressionResult?,
    maxDecimalPlaces: Int,
    minDecimalPlaces: Int = 0,
    locale: Locale
): String {
    if (evaluationResult !is ExpressionResult.Success) return ""

    val hasOperator = expressionBuffer.any {
        it in listOf('+', '−', '-', '×', '*', '÷', '/')
    }
    if (!hasOperator) return ""

    val formattedValue = evaluationResult.value.stripTrailingZeros().formatForDisplay(
        locale = locale,
        maxDecimalPlaces = maxDecimalPlaces,
        minDecimalPlaces = minDecimalPlaces
    )
    return "= $formattedValue"
}
