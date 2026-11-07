package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val dateTimeFormatterCache = ConcurrentHashMap<Pair<String, Locale>, DateTimeFormatter>()

internal fun getDateTimeFormatterCacheSize(): Int = dateTimeFormatterCache.size
internal fun clearDateFormatterCache() {
    dateTimeFormatterCache.clear()
}

private fun getOrCreateDateTimeFormatter(pattern: String, locale: Locale): DateTimeFormatter =
    dateTimeFormatterCache.computeIfAbsent(pattern to locale) { (p, l) ->
        DateTimeFormatter.ofPattern(p, l)
    }

/**
 * Formats a [LocalDateTime] to a short human-readable date string.
 *
 * Formats as "d MMM" (e.g., "10 Nov") if [year] matches [referenceYear].
 * Formats as "d MMM yyyy" (e.g., "10 Nov 2025") if [year] differs from [referenceYear].
 *
 * @param locale The locale to use for localized month abbreviations.
 * @param referenceYear The reference calendar year to compare against (defaults to current year).
 * @return Locale-formatted short date string.
 */
fun LocalDateTime.formatShortDate(
    locale: Locale,
    referenceYear: Int = LocalDate.now().year
): String {
    val pattern = if (year == referenceYear) "d MMM" else "d MMM yyyy"
    val formatter = getOrCreateDateTimeFormatter(pattern, locale)
    return format(formatter)
}

fun LocalDateTime.formatMediumDate(locale: Locale): String {
    val formatter = getOrCreateDateTimeFormatter("MMMM yyyy", locale)
    return format(formatter)
}

/**
 * Formats a [LocalDate] to a short human-readable date string.
 *
 * Formats as "d MMM" (e.g., "10 Nov") if [year] matches [referenceYear].
 * Formats as "d MMM yyyy" (e.g., "10 Nov 2025") if [year] differs from [referenceYear].
 *
 * @param locale The locale to use for localized month abbreviations.
 * @param referenceYear The reference calendar year to compare against (defaults to current year).
 * @return Locale-formatted short date string.
 */
fun LocalDate.formatShortDate(
    locale: Locale,
    referenceYear: Int = LocalDate.now().year
): String {
    val pattern = if (year == referenceYear) "d MMM" else "d MMM yyyy"
    val formatter = getOrCreateDateTimeFormatter(pattern, locale)
    return format(formatter)
}

fun LocalDate.formatMediumDate(locale: Locale): String {
    val formatter = getOrCreateDateTimeFormatter("MMMM yyyy", locale)
    return format(formatter)
}
