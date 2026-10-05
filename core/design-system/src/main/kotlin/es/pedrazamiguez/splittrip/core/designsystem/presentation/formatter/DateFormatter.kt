package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
    return format(formatter)
}

fun LocalDateTime.formatMediumDate(locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
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
    val formatter = DateTimeFormatter.ofPattern(pattern, locale)
    return format(formatter)
}

fun LocalDate.formatMediumDate(locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    return format(formatter)
}
