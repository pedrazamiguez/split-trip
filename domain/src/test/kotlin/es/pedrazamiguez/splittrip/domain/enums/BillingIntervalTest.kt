package es.pedrazamiguez.splittrip.domain.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class BillingIntervalTest {

    @Nested
    inner class FromString {

        @ParameterizedTest
        @CsvSource(
            "MONTHLY, MONTHLY",
            "ANNUAL, ANNUAL",
            "monthly, MONTHLY",
            "annual, ANNUAL",
            "Monthly, MONTHLY",
            "Annual, ANNUAL"
        )
        fun `parses known intervals case-insensitively`(input: String, expected: BillingInterval) {
            assertEquals(expected, BillingInterval.fromString(input))
        }

        @Test
        fun `throws IllegalArgumentException for unknown interval string`() {
            val ex = assertThrows(IllegalArgumentException::class.java) {
                BillingInterval.fromString("WEEKLY")
            }
            assertEquals("Unknown billing interval: WEEKLY", ex.message)
        }
    }

    @Nested
    inner class FromStringOrDefault {

        @ParameterizedTest
        @CsvSource(
            "MONTHLY, MONTHLY",
            "ANNUAL, ANNUAL",
            "monthly, MONTHLY",
            "annual, ANNUAL"
        )
        fun `returns parsed interval for known strings`(input: String, expected: BillingInterval) {
            assertEquals(expected, BillingInterval.fromStringOrDefault(input))
        }

        @Test
        fun `returns ANNUAL for null input`() {
            assertEquals(BillingInterval.ANNUAL, BillingInterval.fromStringOrDefault(null))
        }

        @Test
        fun `returns ANNUAL for invalid input`() {
            assertEquals(BillingInterval.ANNUAL, BillingInterval.fromStringOrDefault("UNKNOWN"))
        }
    }
}
