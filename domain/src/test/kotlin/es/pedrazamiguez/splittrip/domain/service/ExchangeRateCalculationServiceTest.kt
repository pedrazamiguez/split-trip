package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeRateCalculationServiceTest {

    private val service = ExchangeRateCalculationServiceImpl()

    // ── BigDecimal-based methods ─────────────────────────────────────────

    @Test
    fun `calculateGroupAmount multiplies source by rate`() {
        val result = service.calculateGroupAmount(
            sourceAmount = BigDecimal("100.00"),
            rate = BigDecimal("0.027")
        )
        assertEquals(BigDecimal("2.70"), result)
    }

    @Test
    fun `calculateGroupAmount returns zero when rate is zero`() {
        val result = service.calculateGroupAmount(
            sourceAmount = BigDecimal("100.00"),
            rate = BigDecimal.ZERO
        )
        assertEquals(BigDecimal.ZERO, result)
    }

    @Test
    fun `calculateImpliedRate divides target by source`() {
        val result = service.calculateImpliedRate(
            sourceAmount = BigDecimal("1000.00"),
            groupAmount = BigDecimal("27.35")
        )
        assertEquals(BigDecimal("0.027350"), result)
    }

    @Test
    fun `calculateImpliedRate returns zero when source is zero`() {
        val result = service.calculateImpliedRate(
            sourceAmount = BigDecimal.ZERO,
            groupAmount = BigDecimal("27.35")
        )
        assertEquals(BigDecimal.ZERO, result)
    }

    // ── String-based methods ─────────────────────────────────────────────

    @Test
    fun `calculateGroupAmountFromStrings handles valid inputs`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "100.00",
            exchangeRateString = "1.5"
        )
        assertEquals("150.00", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles empty source amount`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "",
            exchangeRateString = "1.5"
        )
        assertEquals("0.00", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles invalid rate defaults to one`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "100.00",
            exchangeRateString = "invalid"
        )
        assertEquals("100.00", result)
    }

    @Test
    fun `calculateImpliedRateFromStrings handles valid inputs`() {
        val result = service.calculateImpliedRateFromStrings(
            sourceAmountString = "1000.00",
            groupAmountString = "27.35"
        )
        assertEquals("0.02735", result)
    }

    @Test
    fun `calculateImpliedRateFromStrings handles empty source amount`() {
        val result = service.calculateImpliedRateFromStrings(
            sourceAmountString = "",
            groupAmountString = "27.35"
        )
        assertEquals("0", result)
    }

    @Test
    fun `calculateImpliedRateFromStrings handles invalid group amount`() {
        val result = service.calculateImpliedRateFromStrings(
            sourceAmountString = "100.00",
            groupAmountString = "invalid"
        )
        assertEquals("0", result)
    }

    // ── Variable decimal places ──────────────────────────────────────────

    @Test
    fun `calculateGroupAmount respects target decimal places for JPY`() {
        val result = service.calculateGroupAmount(
            sourceAmount = BigDecimal("100.00"),
            rate = BigDecimal("157.25"),
            targetDecimalPlaces = 0
        )
        assertEquals(BigDecimal("15725"), result)
    }

    @Test
    fun `calculateGroupAmount respects target decimal places for TND`() {
        val result = service.calculateGroupAmount(
            sourceAmount = BigDecimal("100.00"),
            rate = BigDecimal("3.12345"),
            targetDecimalPlaces = 3
        )
        assertEquals(BigDecimal("312.345"), result)
    }

    @Test
    fun `calculateGroupAmountFromStrings respects source and target decimal places`() {
        // Converting from JPY (0 decimals) to EUR (2 decimals)
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "15725",
            exchangeRateString = "0.00636",
            sourceDecimalPlaces = 0,
            targetDecimalPlaces = 2
        )
        assertEquals("100.01", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles TND to EUR conversion`() {
        // Converting from TND (3 decimals) to EUR (2 decimals)
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "312.345",
            exchangeRateString = "0.32",
            sourceDecimalPlaces = 3,
            targetDecimalPlaces = 2
        )
        assertEquals("99.95", result)
    }

    @Test
    fun `calculateImpliedRateFromStrings respects source decimal places`() {
        // Source is JPY (0 decimals)
        val result = service.calculateImpliedRateFromStrings(
            sourceAmountString = "15725",
            groupAmountString = "100.00",
            sourceDecimalPlaces = 0
        )
        assertEquals("0.006359", result)
    }

    // ── Locale normalization ─────────────────────────────────────────────

    @Test
    fun `calculateGroupAmountFromStrings handles European format with comma as decimal`() {
        // European format: 1.234,56 (dot as thousand separator, comma as decimal)
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "1.234,56",
            exchangeRateString = "1.0"
        )
        assertEquals("1234.56", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles US format with dot as decimal`() {
        // US format: 1,234.56 (comma as thousand separator, dot as decimal)
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "1,234.56",
            exchangeRateString = "1.0"
        )
        assertEquals("1234.56", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles amount without thousand separators`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "1234.56",
            exchangeRateString = "1.0"
        )
        assertEquals("1234.56", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles whole number without decimals`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "15725",
            exchangeRateString = "1.0"
        )
        assertEquals("15725.00", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings preserves precision for TND with 3 decimals`() {
        // TND has 3 decimal places - precision should be preserved
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "12.345",
            exchangeRateString = "1.0",
            sourceDecimalPlaces = 3,
            targetDecimalPlaces = 3
        )
        assertEquals("12.345", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles European format for TND`() {
        // European format with 3 decimal places: 12,345 means 12.345 in TND
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "12,345",
            exchangeRateString = "1.0",
            sourceDecimalPlaces = 3,
            targetDecimalPlaces = 3
        )
        assertEquals("12.345", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles large European format amount`() {
        // 1.234.567,89 in European format = 1234567.89
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "1.234.567,89",
            exchangeRateString = "1.0"
        )
        assertEquals("1234567.89", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles large US format amount`() {
        // 1,234,567.89 in US format = 1234567.89
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "1,234,567.89",
            exchangeRateString = "1.0"
        )
        assertEquals("1234567.89", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings handles whitespace in input`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "  100.50  ",
            exchangeRateString = "1.0"
        )
        assertEquals("100.50", result)
    }

    @Test
    fun `calculateGroupAmountFromStrings returns zero for invalid input`() {
        val result = service.calculateGroupAmountFromStrings(
            sourceAmountString = "invalid",
            exchangeRateString = "1.0"
        )
        assertEquals("0.00", result)
    }

    @Test
    fun `calculateImpliedRateFromStrings handles European format source amount`() {
        val result = service.calculateImpliedRateFromStrings(
            sourceAmountString = "1.000,00",
            groupAmountString = "27.35"
        )
        assertEquals("0.02735", result)
    }

    // ── Display rate methods ─────────────────────────────────────────────

    @Test
    fun `calculateGroupAmountFromDisplayRate converts THB to EUR correctly`() {
        // User enters: 1000 THB, rate: 37 (meaning 1 EUR = 37 THB)
        // Expected: 1000 / 37 = 27.03 EUR
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "1000.00",
            displayRateString = "37.0",
            sourceDecimalPlaces = 2,
            targetDecimalPlaces = 2
        )
        assertEquals("27.03", result)
    }

    @Test
    fun `calculateGroupAmountFromDisplayRate handles rate of 1`() {
        // Same currency, rate is 1
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "100.00",
            displayRateString = "1.0"
        )
        assertEquals("100.00", result)
    }

    @Test
    fun `calculateGroupAmountFromDisplayRate returns zero when rate is zero`() {
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "100.00",
            displayRateString = "0"
        )
        assertEquals("0", result)
    }

    @Test
    fun `calculateGroupAmountFromDisplayRate handles empty source amount`() {
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "",
            displayRateString = "37.0"
        )
        assertEquals("0.00", result)
    }

    @Test
    fun `calculateImpliedDisplayRateFromStrings calculates correct display rate`() {
        // If 1000 THB = 27.03 EUR, the display rate should be ~37 (1 EUR = 37 THB)
        // 1000 / 27.03 = 36.996671
        val result = service.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = "1000.00",
            groupAmountString = "27.03"
        )
        // The result should start with 36.99 (the exact precision may vary)
        assertTrue(result.startsWith("36.99")) { "Expected result starting with 36.99, but got: $result" }
    }

    @Test
    fun `calculateImpliedDisplayRateFromStrings returns zero when target is zero`() {
        val result = service.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = "1000.00",
            groupAmountString = "0"
        )
        assertEquals("0", result)
    }

    @Test
    fun `displayRateToCalculationRate inverts rate correctly`() {
        // Display rate: 37 (1 EUR = 37 THB)
        // Calculation rate should be: 1/37 = 0.027027
        val result = service.displayRateToCalculationRate("37.0")
        assertEquals("0.027027", result.stripTrailingZeros().toPlainString())
    }

    @Test
    fun `displayRateToCalculationRate returns zero when display rate is zero`() {
        val result = service.displayRateToCalculationRate("0")
        assertEquals(BigDecimal.ZERO, result)
    }

    @Test
    fun `displayRateToCalculationRate handles invalid input defaults to one`() {
        val result = service.displayRateToCalculationRate("invalid")
        assertEquals(BigDecimal.ONE.setScale(DomainConstants.RATE_PRECISION), result)
    }

    // ── Locale-specific rate parsing (Spanish) ───────────────────────────

    @Test
    fun `calculateGroupAmountFromDisplayRate handles Spanish locale rate with comma`() {
        // User enters rate with comma as decimal separator: 37,220844 (Spanish format)
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "1000.00",
            displayRateString = "37,220844",
            sourceDecimalPlaces = 2,
            targetDecimalPlaces = 2
        )
        // 1000 / 37.220844 = 26.87 EUR
        assertEquals("26.87", result)
    }

    @Test
    fun `calculateGroupAmountFromDisplayRate handles rate with dot decimal separator`() {
        // User enters rate with dot as decimal separator: 37.220844 (US/UK format)
        val result = service.calculateGroupAmountFromDisplayRate(
            sourceAmountString = "1000.00",
            displayRateString = "37.220844",
            sourceDecimalPlaces = 2,
            targetDecimalPlaces = 2
        )
        // 1000 / 37.220844 = 26.87 EUR
        assertEquals("26.87", result)
    }

    @Test
    fun `displayRateToCalculationRate handles Spanish locale rate with comma`() {
        // Display rate with comma: 37,22 (Spanish format for 37.22)
        val result = service.displayRateToCalculationRate("37,22")
        // 1 / 37.22 = 0.0268672... rounds to 0.026867 with HALF_UP at 6 decimals
        assertEquals("0.026867", result.stripTrailingZeros().toPlainString())
    }

    @Test
    fun `calculateImpliedDisplayRateFromStrings handles Spanish locale amounts`() {
        // Source amount with comma: 1.000,00 (Spanish format for 1000.00)
        val result = service.calculateImpliedDisplayRateFromStrings(
            sourceAmountString = "1.000,00",
            groupAmountString = "27,03"
        )
        // Should calculate: 1000 / 27.03 = ~36.99
        assertTrue(result.startsWith("36.99")) { "Expected result starting with 36.99, but got: $result" }
    }

    // ── calculateExchangeRate ────────────────────────────────────────────

    @Test
    fun `calculateExchangeRate computes correct rate from withdrawal amounts`() {
        // 1000000 THB cents / 27000 EUR cents = 37.037037
        val result = service.calculateExchangeRate(
            amountWithdrawn = 1000000L,
            deductedBaseAmount = 27000L
        )
        assertEquals(0, BigDecimal("37.037037").compareTo(result))
    }

    @Test
    fun `calculateExchangeRate returns ONE for same currency (1-to-1)`() {
        val result = service.calculateExchangeRate(
            amountWithdrawn = 50000L,
            deductedBaseAmount = 50000L
        )
        assertEquals(0, BigDecimal.ONE.compareTo(result))
    }

    @Test
    fun `calculateExchangeRate returns ONE when deductedBaseAmount is zero`() {
        val result = service.calculateExchangeRate(
            amountWithdrawn = 1000000L,
            deductedBaseAmount = 0L
        )
        assertEquals(0, BigDecimal.ONE.compareTo(result))
    }

    @Test
    fun `calculateExchangeRate returns ONE when deductedBaseAmount is negative`() {
        val result = service.calculateExchangeRate(
            amountWithdrawn = 1000000L,
            deductedBaseAmount = -100L
        )
        assertEquals(0, BigDecimal.ONE.compareTo(result))
    }

    // ── Blended Rate ─────────────────────────────────────────────────────

    @Test
    fun `calculateBlendedRate returns correct internal rate`() {
        // 1000 THB (100000 cents) = 27 EUR (2700 cents) → internal rate = 2700 / 100000 = 0.027
        val result = service.calculateBlendedRate(
            sourceAmountCents = 100000L,
            groupAmountCents = 2700L
        )
        assertEquals(BigDecimal("0.027000"), result)
    }

    @Test
    fun `calculateBlendedRate returns ONE when source is zero`() {
        val result = service.calculateBlendedRate(sourceAmountCents = 0L, groupAmountCents = 2700L)
        assertEquals(BigDecimal.ONE, result)
    }

    @Test
    fun `calculateBlendedRate returns ONE when group is zero`() {
        val result = service.calculateBlendedRate(sourceAmountCents = 100000L, groupAmountCents = 0L)
        assertEquals(BigDecimal.ONE, result)
    }

    @Test
    fun `calculateBlendedRate returns ONE when both are negative`() {
        val result = service.calculateBlendedRate(sourceAmountCents = -1L, groupAmountCents = -1L)
        assertEquals(BigDecimal.ONE, result)
    }

    @Test
    fun `calculateBlendedDisplayRate returns correct display rate`() {
        // 1000 THB (100000 cents) = 27 EUR (2700 cents) → display rate = 100000 / 2700 ≈ 37.037037
        val result = service.calculateBlendedDisplayRate(
            sourceAmountCents = 100000L,
            groupAmountCents = 2700L
        )
        assertEquals(BigDecimal("37.037037"), result)
    }

    @Test
    fun `calculateBlendedDisplayRate returns ONE when source is zero`() {
        val result = service.calculateBlendedDisplayRate(sourceAmountCents = 0L, groupAmountCents = 2700L)
        assertEquals(BigDecimal.ONE, result)
    }

    @Test
    fun `calculateBlendedDisplayRate returns ONE when group is zero`() {
        val result = service.calculateBlendedDisplayRate(sourceAmountCents = 100000L, groupAmountCents = 0L)
        assertEquals(BigDecimal.ONE, result)
    }

    @Test
    fun `blended rates are inverses of each other`() {
        val sourceAmountCents = 175000L // 1750 THB
        val groupAmountCents = 4752L // 47.52 EUR

        val internalRate = service.calculateBlendedRate(sourceAmountCents, groupAmountCents)
        val displayRate = service.calculateBlendedDisplayRate(sourceAmountCents, groupAmountCents)

        // internal * display ≈ 1.0 (within rounding tolerance)
        val product = internalRate.multiply(displayRate)
        assertTrue(
            product.subtract(BigDecimal.ONE).abs() < BigDecimal("0.001"),
            "Expected internal * display ≈ 1.0, got $product"
        )
    }
}
