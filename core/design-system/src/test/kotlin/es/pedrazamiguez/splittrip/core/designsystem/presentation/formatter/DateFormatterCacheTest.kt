package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DateFormatter Caching")
class DateFormatterCacheTest {

    private val esLocale = Locale.forLanguageTag("es-ES")
    private val usLocale = Locale.US

    @BeforeEach
    fun setUp() {
        clearDateFormatterCache()
    }

    @Test
    fun `dateTimeFormatterCache reuses cached formatter instances`() {
        val dateTime = LocalDateTime.of(2026, 11, 7, 10, 30)

        assertEquals(0, getDateTimeFormatterCacheSize())

        val firstFormatted = dateTime.formatShortDate(esLocale, referenceYear = 2026)
        assertEquals(1, getDateTimeFormatterCacheSize())

        repeat(100) {
            val nextFormatted = dateTime.formatShortDate(esLocale, referenceYear = 2026)
            assertEquals(firstFormatted, nextFormatted)
        }

        assertEquals(1, getDateTimeFormatterCacheSize())
    }

    @Test
    fun `formatMediumDate uses and populates cache across calls`() {
        val dateTime = LocalDateTime.of(2026, 11, 7, 10, 30)
        val date = LocalDate.of(2026, 11, 7)

        val formattedDateTime = dateTime.formatMediumDate(usLocale)
        val formattedDate = date.formatMediumDate(usLocale)

        assertEquals(formattedDateTime, formattedDate)
        assertEquals("November 2026", formattedDateTime)
        // Only one pattern ("MMMM yyyy" with US locale) should be cached
        assertEquals(1, getDateTimeFormatterCacheSize())
    }

    @Test
    fun `different locales and patterns populate distinct cache entries`() {
        val date = LocalDate.of(2025, 5, 1)

        date.formatShortDate(usLocale, referenceYear = 2026) // "d MMM yyyy" with US
        date.formatShortDate(esLocale, referenceYear = 2026) // "d MMM yyyy" with ES
        date.formatShortDate(usLocale, referenceYear = 2025) // "d MMM" with US
        date.formatMediumDate(usLocale) // "MMMM yyyy" with US

        assertEquals(4, getDateTimeFormatterCacheSize())
    }
}
