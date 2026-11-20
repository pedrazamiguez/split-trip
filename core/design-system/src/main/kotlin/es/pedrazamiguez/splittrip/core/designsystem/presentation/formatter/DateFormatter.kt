package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val SHORT_TIME_CACHE_KEY = "LOCALIZED_SHORT_TIME"

private val dateTimeFormatterCache = ConcurrentHashMap<PatternLocaleCacheKey, DateTimeFormatter>()

internal fun getDateTimeFormatterCacheSize(): Int = dateTimeFormatterCache.size
internal fun clearDateFormatterCache() {
    dateTimeFormatterCache.clear()
}

private fun getOrCreateDateTimeFormatter(pattern: String, locale: Locale): DateTimeFormatter =
    dateTimeFormatterCache.computeIfAbsent(PatternLocaleCacheKey(pattern, locale)) { (p, l) ->
        DateTimeFormatter.ofPattern(p, l)
    }

private fun getOrCreateShortTimeFormatter(locale: Locale): DateTimeFormatter =
    dateTimeFormatterCache.computeIfAbsent(PatternLocaleCacheKey(SHORT_TIME_CACHE_KEY, locale)) { (_, l) ->
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(l)
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
 * Formats a [LocalDateTime] to a short human-readable date and localized time string.
 *
 * Formats as "d MMM, <short time>" (e.g., "21 oct, 14:30" or "21 Oct, 2:30 PM") if [year]
 * matches [referenceYear].
 * Formats as "d MMM yyyy, <short time>" (e.g., "21 oct 2025, 14:30" or "21 Oct 2025, 2:30 PM")
 * if [year] differs from [referenceYear].
 *
 * Expects a [LocalDateTime] already converted to the target display timezone
 * (e.g., via `utcToSystemLocalDateTime(zoneId)`).
 *
 * @param locale The locale to use for localized month abbreviations and 12h/24h time format.
 * @param referenceYear The reference calendar year to compare against (defaults to current year).
 * @return Locale-formatted short date and time string.
 */
fun LocalDateTime.formatShortDateTime(
    locale: Locale,
    referenceYear: Int = LocalDate.now().year
): String {
    val formattedDate = formatShortDate(locale, referenceYear)
    val formattedTime = format(getOrCreateShortTimeFormatter(locale))
    return "$formattedDate, $formattedTime"
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
