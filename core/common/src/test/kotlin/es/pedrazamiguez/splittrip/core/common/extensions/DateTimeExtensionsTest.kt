package es.pedrazamiguez.splittrip.core.common.extensions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DateTimeExtensionsTest {

    private lateinit var originalTimeZone: TimeZone

    @BeforeEach
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
    }

    @AfterEach
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Nested
    @DisplayName("toLocalDateTimeUtc")
    inner class ToLocalDateTimeUtc {

        @Test
        fun `epoch zero maps to 1970-01-01 00_00`() {
            val result = 0L.toLocalDateTimeUtc()

            assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0, 0), result)
        }

        @Test
        fun `known epoch millis maps to correct UTC date time`() {
            // 2026-03-15T10:30:00 UTC = 1_773_570_600_000L
            val millis = 1_773_570_600_000L
            val expected = LocalDateTime.of(2026, 3, 15, 10, 30, 0)

            val result = millis.toLocalDateTimeUtc()

            assertEquals(expected, result)
        }

        @Test
        fun `result is timezone-independent`() {
            val millis = 1_773_570_600_000L
            val expected = LocalDateTime.of(2026, 3, 15, 10, 30, 0)

            // Set device timezone to Tokyo (UTC+9) — must not affect the result
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))

            val result = millis.toLocalDateTimeUtc()

            assertEquals(expected, result)
        }
    }

    @Nested
    @DisplayName("toEpochMillisUtc")
    inner class ToEpochMillisUtc {

        @Test
        fun `1970-01-01 00_00 maps to epoch zero`() {
            val dateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

            val result = dateTime.toEpochMillisUtc()

            assertEquals(0L, result)
        }

        @Test
        fun `known UTC date time maps to correct epoch millis`() {
            val dateTime = LocalDateTime.of(2026, 3, 15, 10, 30, 0)

            val result = dateTime.toEpochMillisUtc()

            assertEquals(1_773_570_600_000L, result)
        }

        @Test
        fun `result is timezone-independent`() {
            val dateTime = LocalDateTime.of(2026, 3, 15, 10, 30, 0)

            // Set device timezone to Los Angeles (UTC-7/UTC-8) — must not affect the result
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))

            val result = dateTime.toEpochMillisUtc()

            assertEquals(1_773_570_600_000L, result)
        }
    }

    @Nested
    @DisplayName("Round-trip fidelity")
    inner class RoundTrip {

        @Test
        fun `Long to LocalDateTime to Long round-trips correctly`() {
            val originalMillis = 1_773_570_600_000L

            val roundTripped = originalMillis.toLocalDateTimeUtc().toEpochMillisUtc()

            assertEquals(originalMillis, roundTripped)
        }

        @Test
        fun `LocalDateTime to Long to LocalDateTime round-trips correctly`() {
            val originalDateTime = LocalDateTime.of(2026, 3, 15, 10, 30, 0)

            val roundTripped = originalDateTime.toEpochMillisUtc().toLocalDateTimeUtc()

            assertEquals(originalDateTime, roundTripped)
        }

        @Test
        fun `round-trip is timezone-independent`() {
            TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"))
            val originalMillis = 1_773_570_600_000L

            val roundTripped = originalMillis.toLocalDateTimeUtc().toEpochMillisUtc()

            assertEquals(originalMillis, roundTripped)
        }
    }

    @Nested
    @DisplayName("utcToSystemLocalDateTime")
    inner class UtcToSystemLocalDateTime {

        @Test
        fun `utcToSystemLocalDateTime converts UTC to positive offset timezone across midnight boundary`() {
            val utcDateTime = LocalDateTime.of(2026, 10, 27, 22, 58, 0)
            val shanghaiZone = ZoneId.of("Asia/Shanghai")

            val result = utcDateTime.utcToSystemLocalDateTime(shanghaiZone)

            assertEquals(LocalDateTime.of(2026, 10, 28, 6, 58, 0), result)
        }

        @Test
        fun `utcToSystemLocalDateTime converts UTC to negative offset timezone across midnight boundary`() {
            val utcDateTime = LocalDateTime.of(2026, 10, 28, 2, 0, 0)
            val newYorkZone = ZoneId.of("America/New_York")

            val result = utcDateTime.utcToSystemLocalDateTime(newYorkZone)

            assertEquals(LocalDateTime.of(2026, 10, 27, 22, 0, 0), result)
        }

        @Test
        fun `utcToSystemLocalDateTime preserves date time when zone is UTC`() {
            val utcDateTime = LocalDateTime.of(2026, 10, 28, 12, 0, 0)

            val result = utcDateTime.utcToSystemLocalDateTime(ZoneOffset.UTC)

            assertEquals(LocalDateTime.of(2026, 10, 28, 12, 0, 0), result)
        }

        @Test
        fun `utcToSystemLocalDateTime defaults to systemDefault zone when zoneId omitted`() {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val utcDateTime = LocalDateTime.of(2026, 10, 27, 22, 58, 0)

            val result = utcDateTime.utcToSystemLocalDateTime()

            assertEquals(LocalDateTime.of(2026, 10, 28, 6, 58, 0), result)
        }
    }

    @Nested
    @DisplayName("utcToSystemLocalDate")
    inner class UtcToSystemLocalDate {

        @Test
        fun `utcToSystemLocalDate converts UTC to positive offset timezone across midnight boundary`() {
            val utcDateTime = LocalDateTime.of(2026, 10, 27, 22, 58, 0)
            val shanghaiZone = ZoneId.of("Asia/Shanghai")

            val result = utcDateTime.utcToSystemLocalDate(shanghaiZone)

            assertEquals(LocalDate.of(2026, 10, 28), result)
        }

        @Test
        fun `utcToSystemLocalDate converts UTC to negative offset timezone across midnight boundary`() {
            val utcDateTime = LocalDateTime.of(2026, 10, 28, 2, 0, 0)
            val newYorkZone = ZoneId.of("America/New_York")

            val result = utcDateTime.utcToSystemLocalDate(newYorkZone)

            assertEquals(LocalDate.of(2026, 10, 27), result)
        }

        @Test
        fun `utcToSystemLocalDate defaults to systemDefault zone when zoneId omitted`() {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val utcDateTime = LocalDateTime.of(2026, 10, 27, 22, 58, 0)

            val result = utcDateTime.utcToSystemLocalDate()

            assertEquals(LocalDate.of(2026, 10, 28), result)
        }
    }
}
