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
    }
}
