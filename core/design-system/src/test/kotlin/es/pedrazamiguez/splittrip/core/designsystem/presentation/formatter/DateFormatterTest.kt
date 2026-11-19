package es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DateFormatter and FormattingHelper")
class DateFormatterTest {

    private val usLocale = Locale.US
    private val esLocale = Locale.forLanguageTag("es-ES")

    private val testLocalDateTime = LocalDateTime.of(2026, 6, 15, 14, 30)
    private val testLocalDate = LocalDate.of(2026, 6, 15)

    private fun String.normalizeSpaces(): String =
        replace('\u202F', ' ').replace('\u00A0', ' ')

    @Nested
    @DisplayName("LocalDateTime formatting")
    inner class LocalDateTimeFormatting {

        @Test
        fun `formatShortDate formats correctly without year when in reference year with US locale`() {
            val result = testLocalDateTime.formatShortDate(usLocale, referenceYear = 2026)
            assertEquals("15 Jun", result)
        }

        @Test
        fun `formatShortDate formats correctly without year when in reference year with Spanish locale`() {
            val result = testLocalDateTime.formatShortDate(esLocale, referenceYear = 2026)
            assertEquals("15 jun", result.lowercase())
        }

        @Test
        fun `formatShortDate includes 4-digit year when in past year with US locale`() {
            val result = testLocalDateTime.formatShortDate(usLocale, referenceYear = 2027)
            assertEquals("15 Jun 2026", result)
        }

        @Test
        fun `formatShortDate includes 4-digit year when in past year with Spanish locale`() {
            val result = testLocalDateTime.formatShortDate(esLocale, referenceYear = 2027)
            assertEquals("15 jun 2026", result.lowercase())
        }

        @Test
        fun `formatShortDate includes 4-digit year when in future year with US locale`() {
            val result = testLocalDateTime.formatShortDate(usLocale, referenceYear = 2025)
            assertEquals("15 Jun 2026", result)
        }

        @Test
        fun `formatShortDate defaults referenceYear to current year`() {
            val currentYear = LocalDate.now().year
            val currentYearDateTime = LocalDateTime.of(currentYear, 6, 15, 12, 0)
            assertEquals("15 Jun", currentYearDateTime.formatShortDate(usLocale))

            val pastYearDateTime = LocalDateTime.of(currentYear - 1, 6, 15, 12, 0)
            assertEquals("15 Jun ${currentYear - 1}", pastYearDateTime.formatShortDate(usLocale))
        }

        @Test
        fun `formatMediumDate formats correctly with US locale`() {
            val result = testLocalDateTime.formatMediumDate(usLocale)
            assertEquals("June 2026", result)
        }

        @Test
        fun `formatMediumDate formats correctly with Spanish locale`() {
            val result = testLocalDateTime.formatMediumDate(esLocale)
            assertEquals("junio 2026", result.lowercase())
        }

        @Test
        fun `formatShortDateTime formats date and 12h time without year when in reference year with US locale`() {
            val result = testLocalDateTime.formatShortDateTime(usLocale, referenceYear = 2026)
            assertEquals("15 Jun, 2:30 PM", result.normalizeSpaces())
        }

        @Test
        fun `formatShortDateTime formats date and 24h time without year when in reference year with Spanish locale`() {
            val result = testLocalDateTime.formatShortDateTime(esLocale, referenceYear = 2026)
            assertEquals("15 jun, 14:30", result.lowercase())
        }

        @Test
        fun `formatShortDateTime includes 4-digit year and time when in past year with US locale`() {
            val result = testLocalDateTime.formatShortDateTime(usLocale, referenceYear = 2027)
            assertEquals("15 Jun 2026, 2:30 PM", result.normalizeSpaces())
        }

        @Test
        fun `formatShortDateTime includes 4-digit year and time when in past year with Spanish locale`() {
            val result = testLocalDateTime.formatShortDateTime(esLocale, referenceYear = 2027)
            assertEquals("15 jun 2026, 14:30", result.lowercase())
        }

        @Test
        fun `formatShortDateTime includes 4-digit year and time when in future year with US locale`() {
            val result = testLocalDateTime.formatShortDateTime(usLocale, referenceYear = 2025)
            assertEquals("15 Jun 2026, 2:30 PM", result.normalizeSpaces())
        }

        @Test
        fun `formatShortDateTime defaults referenceYear to current year`() {
            val currentYear = LocalDate.now().year
            val currentYearDateTime = LocalDateTime.of(currentYear, 6, 15, 14, 30)
            assertEquals(
                "15 Jun, 2:30 PM",
                currentYearDateTime.formatShortDateTime(usLocale).normalizeSpaces()
            )

            val pastYearDateTime = LocalDateTime.of(currentYear - 1, 6, 15, 14, 30)
            assertEquals(
                "15 Jun ${currentYear - 1}, 2:30 PM",
                pastYearDateTime.formatShortDateTime(usLocale).normalizeSpaces()
            )
        }

        @Test
        fun `formatShortDateTime formats morning and midnight times correctly across locales`() {
            val morning = LocalDateTime.of(2026, 10, 28, 6, 58)
            val midnight = LocalDateTime.of(2026, 10, 28, 0, 5)

            assertEquals(
                "28 Oct, 6:58 AM",
                morning.formatShortDateTime(usLocale, referenceYear = 2026).normalizeSpaces()
            )
            assertEquals(
                "28 Oct, 12:05 AM",
                midnight.formatShortDateTime(usLocale, referenceYear = 2026).normalizeSpaces()
            )
            assertEquals("28 oct, 6:58", morning.formatShortDateTime(esLocale, referenceYear = 2026).lowercase())
            assertEquals("28 oct, 0:05", midnight.formatShortDateTime(esLocale, referenceYear = 2026).lowercase())
        }
    }

    @Nested
    @DisplayName("LocalDate formatting")
    inner class LocalDateFormatting {

        @Test
        fun `formatShortDate formats correctly without year when in reference year with US locale`() {
            val result = testLocalDate.formatShortDate(usLocale, referenceYear = 2026)
            assertEquals("15 Jun", result)
        }

        @Test
        fun `formatShortDate formats correctly without year when in reference year with Spanish locale`() {
            val result = testLocalDate.formatShortDate(esLocale, referenceYear = 2026)
            assertEquals("15 jun", result.lowercase())
        }

        @Test
        fun `formatShortDate includes 4-digit year when in past year with US locale`() {
            val result = testLocalDate.formatShortDate(usLocale, referenceYear = 2027)
            assertEquals("15 Jun 2026", result)
        }

        @Test
        fun `formatShortDate includes 4-digit year when in past year with Spanish locale`() {
            val result = testLocalDate.formatShortDate(esLocale, referenceYear = 2027)
            assertEquals("15 jun 2026", result.lowercase())
        }

        @Test
        fun `formatShortDate includes 4-digit year when in future year with US locale`() {
            val result = testLocalDate.formatShortDate(usLocale, referenceYear = 2025)
            assertEquals("15 Jun 2026", result)
        }

        @Test
        fun `formatShortDate defaults referenceYear to current year`() {
            val currentYear = LocalDate.now().year
            val currentYearDate = LocalDate.of(currentYear, 6, 15)
            assertEquals("15 Jun", currentYearDate.formatShortDate(usLocale))

            val pastYearDate = LocalDate.of(currentYear - 1, 6, 15)
            assertEquals("15 Jun ${currentYear - 1}", pastYearDate.formatShortDate(usLocale))
        }

        @Test
        fun `formatMediumDate formats correctly with US locale`() {
            val result = testLocalDate.formatMediumDate(usLocale)
            assertEquals("June 2026", result)
        }

        @Test
        fun `formatMediumDate formats correctly with Spanish locale`() {
            val result = testLocalDate.formatMediumDate(esLocale)
            assertEquals("junio 2026", result.lowercase())
        }
    }

    @Nested
    @DisplayName("FormattingHelper date delegation")
    inner class FormattingHelperDateDelegation {

        private val localeProvider: LocaleProvider = mockk {
            every { getCurrentLocale() } returns usLocale
        }
        private val helper = FormattingHelper(localeProvider = localeProvider)

        @Test
        fun `formatShortDate with LocalDateTime formats same year or returns empty on null`() {
            assertEquals("15 Jun", helper.formatShortDate(testLocalDateTime, referenceYear = 2026))
            assertEquals("", helper.formatShortDate(null as LocalDateTime?, referenceYear = 2026))
        }

        @Test
        fun `formatShortDate with LocalDateTime formats different year`() {
            assertEquals("15 Jun 2026", helper.formatShortDate(testLocalDateTime, referenceYear = 2027))
        }

        @Test
        fun `formatShortDate with LocalDate formats same year or returns empty on null`() {
            assertEquals("15 Jun", helper.formatShortDate(testLocalDate, referenceYear = 2026))
            assertEquals("", helper.formatShortDate(null as LocalDate?, referenceYear = 2026))
        }

        @Test
        fun `formatShortDate with LocalDate formats different year`() {
            assertEquals("15 Jun 2026", helper.formatShortDate(testLocalDate, referenceYear = 2027))
        }

        @Test
        fun `formatMediumDate with LocalDate formats or returns empty on null`() {
            assertEquals("June 2026", helper.formatMediumDate(testLocalDate))
            assertEquals("", helper.formatMediumDate(null as LocalDate?))
        }

        @Test
        fun `formatShortDateTime with LocalDateTime formats same year or returns empty on null`() {
            assertEquals(
                "15 Jun, 2:30 PM",
                helper.formatShortDateTime(testLocalDateTime, referenceYear = 2026).normalizeSpaces()
            )
            assertEquals("", helper.formatShortDateTime(null, referenceYear = 2026))
        }

        @Test
        fun `formatShortDateTime with LocalDateTime formats different year`() {
            assertEquals(
                "15 Jun 2026, 2:30 PM",
                helper.formatShortDateTime(testLocalDateTime, referenceYear = 2027).normalizeSpaces()
            )
        }

        @Test
        fun `formatShortDateTime delegates to active Spanish locale from LocaleProvider`() {
            every { localeProvider.getCurrentLocale() } returns esLocale
            val octoberDateTime = LocalDateTime.of(2026, 10, 21, 14, 30)

            assertEquals("21 oct, 14:30", helper.formatShortDateTime(octoberDateTime, referenceYear = 2026).lowercase())
            assertEquals(
                "21 oct 2026, 14:30",
                helper.formatShortDateTime(octoberDateTime, referenceYear = 2027).lowercase()
            )
        }
    }
}
