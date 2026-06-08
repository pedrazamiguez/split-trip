package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddOnCalculationServiceTest {

    private val addOnService = AddOnCalculationServiceImpl()

    // ── calculateTotalOnTopAddOns ─────────────────────────────────────────

    @Test
    fun `calculateTotalOnTopAddOns sums non-discount ON_TOP add-ons`() {
        val addOns = listOf(
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 250),
            AddOn(type = AddOnType.TIP, mode = AddOnMode.ON_TOP, groupAmountCents = 500),
            AddOn(type = AddOnType.SURCHARGE, mode = AddOnMode.ON_TOP, groupAmountCents = 100)
        )
        assertEquals(850, addOnService.calculateTotalOnTopAddOns(addOns))
    }

    @Test
    fun `calculateTotalOnTopAddOns excludes INCLUDED add-ons`() {
        val addOns = listOf(
            AddOn(type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 500),
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 250)
        )
        assertEquals(250, addOnService.calculateTotalOnTopAddOns(addOns))
    }

    @Test
    fun `calculateTotalOnTopAddOns excludes DISCOUNT add-ons`() {
        val addOns = listOf(
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, groupAmountCents = 300),
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 100)
        )
        assertEquals(100, addOnService.calculateTotalOnTopAddOns(addOns))
    }

    @Test
    fun `calculateTotalOnTopAddOns returns zero for empty list`() {
        assertEquals(0, addOnService.calculateTotalOnTopAddOns(emptyList()))
    }

    // ── calculateTotalAddOnExtras ─────────────────────────────────────────

    @Test
    fun `calculateTotalAddOnExtras sums ON_TOP and INCLUDED non-discount add-ons`() {
        val addOns = listOf(
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 250),
            AddOn(type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 500),
            AddOn(type = AddOnType.SURCHARGE, mode = AddOnMode.ON_TOP, groupAmountCents = 100)
        )
        // 250 + 500 + 100 = 850
        assertEquals(850, addOnService.calculateTotalAddOnExtras(addOns))
    }

    @Test
    fun `calculateTotalAddOnExtras excludes DISCOUNT add-ons regardless of mode`() {
        val addOns = listOf(
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, groupAmountCents = 300),
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.INCLUDED, groupAmountCents = 200),
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 100)
        )
        // Only FEE counts
        assertEquals(100, addOnService.calculateTotalAddOnExtras(addOns))
    }

    @Test
    fun `calculateTotalAddOnExtras returns zero for empty list`() {
        assertEquals(0, addOnService.calculateTotalAddOnExtras(emptyList()))
    }

    @Test
    fun `calculateTotalAddOnExtras INCLUDED-only returns their sum`() {
        val addOns = listOf(
            AddOn(type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 727),
            AddOn(type = AddOnType.FEE, mode = AddOnMode.INCLUDED, groupAmountCents = 100)
        )
        assertEquals(827, addOnService.calculateTotalAddOnExtras(addOns))
    }

    // ── calculateEffectiveGroupAmount ─────────────────────────────────────

    @Test
    fun `calculateEffectiveGroupAmount returns base when no add-ons`() {
        assertEquals(10000L, addOnService.calculateEffectiveGroupAmount(10000L, emptyList()))
    }

    @Test
    fun `calculateEffectiveGroupAmount adds ON_TOP fee to base`() {
        val addOns = listOf(
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 250)
        )
        // 10000 + 250 = 10250
        assertEquals(10250L, addOnService.calculateEffectiveGroupAmount(10000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount subtracts ON_TOP DISCOUNT from base`() {
        val addOns = listOf(
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, groupAmountCents = 500)
        )
        // 10000 - 500 = 9500
        assertEquals(9500L, addOnService.calculateEffectiveGroupAmount(10000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount ignores INCLUDED DISCOUNT`() {
        // INCLUDED discounts are informational — they don't reduce effective amount
        val addOns = listOf(
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.INCLUDED, groupAmountCents = 500)
        )
        assertEquals(10000L, addOnService.calculateEffectiveGroupAmount(10000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount INCLUDED tip adds back to base`() {
        // Stored base cost = 9000, INCLUDED tip = 1000 → effective = 10000 (reconstructs original total)
        val addOns = listOf(
            AddOn(type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 1000)
        )
        assertEquals(10000L, addOnService.calculateEffectiveGroupAmount(9000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount handles mixed add-ons correctly`() {
        // Scenario: 100.00 EUR base + 10 EUR tip on top + 2.50 EUR fee + 8 EUR tip included
        //           − 5 EUR ON_TOP discount (subtracted) + 3 EUR INCLUDED discount (ignored)
        val addOns = listOf(
            AddOn(type = AddOnType.TIP, mode = AddOnMode.ON_TOP, groupAmountCents = 1000),
            AddOn(type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 250),
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, groupAmountCents = 500),
            AddOn(type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 800),
            AddOn(type = AddOnType.DISCOUNT, mode = AddOnMode.INCLUDED, groupAmountCents = 300)
        )
        // 10000 + 1000 + 250 + 800 - 500 = 11550 (INCLUDED discount ignored)
        assertEquals(11550L, addOnService.calculateEffectiveGroupAmount(10000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount scenario E1 - boat with foreign fee`() {
        // E1: 4000 MXN boat, bank fee 2.50 EUR
        // Base group amount: 200.00 EUR (4000 MXN converted)
        // Fee add-on: 250 cents (2.50 EUR, already in group currency)
        val addOns = listOf(
            AddOn(
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                amountCents = 250,
                currency = "EUR",
                groupAmountCents = 250
            )
        )
        assertEquals(20250L, addOnService.calculateEffectiveGroupAmount(20000L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount scenario E3a - tip already included`() {
        // E3a: User entered 80 USD total with 10% tip included
        // Base cost: 80 / 1.10 = 72.73 USD → groupAmount = 6545 (at 0.9 rate)
        // Tip: 72.73 * 10% = 7.27 USD → groupAmountCents = 655
        // Effective reconstructs the original total: 6545 + 655 = 7200
        val addOns = listOf(
            AddOn(
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                amountCents = 727,
                groupAmountCents = 655
            )
        )
        assertEquals(7200L, addOnService.calculateEffectiveGroupAmount(6545L, addOns))
    }

    @Test
    fun `calculateEffectiveGroupAmount scenario E3b - tip on top`() {
        // E3b: 72 USD dinner + 10% tip = 7.20 USD → total 79.20 USD
        // groupAmount for base = 6480 (72 * 0.9), tip groupAmount = 648
        val addOns = listOf(
            AddOn(
                type = AddOnType.TIP,
                mode = AddOnMode.ON_TOP,
                amountCents = 720,
                groupAmountCents = 648
            )
        )
        assertEquals(7128L, addOnService.calculateEffectiveGroupAmount(6480L, addOns))
    }

    // ── calculateEffectiveDeductedAmount ──────────────────────────────────

    @Test
    fun `calculateEffectiveDeductedAmount returns base when no add-ons`() {
        assertEquals(27000L, addOnService.calculateEffectiveDeductedAmount(27000L, emptyList()))
    }

    @Test
    fun `calculateEffectiveDeductedAmount adds ATM fee`() {
        // E2: 5000 THB withdrawal, ATM charges 260 THB fee
        // deductedBaseAmount = 135.87 EUR (withdrawal), fee groupAmount = 7.06 EUR = 706 cents
        val addOns = listOf(
            AddOn(
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                amountCents = 26000, // 260 THB
                currency = "THB",
                groupAmountCents = 706
            )
        )
        assertEquals(14293L, addOnService.calculateEffectiveDeductedAmount(13587L, addOns))
    }

    @Test
    fun `calculateEffectiveDeductedAmount ignores INCLUDED add-ons`() {
        val addOns = listOf(
            AddOn(type = AddOnType.FEE, mode = AddOnMode.INCLUDED, groupAmountCents = 500)
        )
        assertEquals(27000L, addOnService.calculateEffectiveDeductedAmount(27000L, addOns))
    }

    // ── calculateIncludedBaseCost ─────────────────────────────────────────

    @Test
    fun `calculateIncludedBaseCost returns total when no included amounts`() {
        assertEquals(
            8000L,
            addOnService.calculateIncludedBaseCost(8000L, 0L, BigDecimal.ZERO)
        )
    }

    @Test
    fun `calculateIncludedBaseCost subtracts exact included amount`() {
        // 80 EUR − 10 EUR included fee = 70 EUR base → 7000 cents
        assertEquals(
            7000L,
            addOnService.calculateIncludedBaseCost(8000L, 1000L, BigDecimal.ZERO)
        )
    }

    @Test
    fun `calculateIncludedBaseCost extracts percentage included amount`() {
        // 80 EUR includes 20% tip → base = 80 / 1.20 = 66.67 EUR → 6667 cents
        assertEquals(
            6667L,
            addOnService.calculateIncludedBaseCost(8000L, 0L, BigDecimal("20"))
        )
    }

    @Test
    fun `calculateIncludedBaseCost handles mixed exact and percentage`() {
        // 100 EUR − 5 EUR fee (exact) = 95 EUR, then 95 / 1.10 (10% tip) ≈ 86.36 → 8636 cents
        assertEquals(
            8636L,
            addOnService.calculateIncludedBaseCost(10000L, 500L, BigDecimal("10"))
        )
    }

    @Test
    fun `calculateIncludedBaseCost never returns negative`() {
        // Exact included exceeds total → coerced to 0
        assertEquals(
            0L,
            addOnService.calculateIncludedBaseCost(1000L, 5000L, BigDecimal.ZERO)
        )
    }

    @Test
    fun `calculateIncludedBaseCost with small percentage`() {
        // 50 EUR includes 5% surcharge → base = 50 / 1.05 = 47.62 → 4762 cents
        assertEquals(
            4762L,
            addOnService.calculateIncludedBaseCost(5000L, 0L, BigDecimal("5"))
        )
    }

    @Test
    fun `calculateIncludedBaseCost does not crash on minus 100 percent`() {
        // -100% → divisor = 0 → guarded, falls back to afterExact
        assertEquals(
            8000L,
            addOnService.calculateIncludedBaseCost(8000L, 0L, BigDecimal("-100"))
        )
    }

    @Test
    fun `calculateIncludedBaseCost does not crash on percentage below minus 100`() {
        // -200% → divisor = -1 → guarded, falls back to afterExact
        assertEquals(
            8000L,
            addOnService.calculateIncludedBaseCost(8000L, 0L, BigDecimal("-200"))
        )
    }

    @Test
    fun `calculateIncludedBaseCost reverses INCLUDED DISCOUNT percentage`() {
        // Bug #824: 90 EUR with 10% INCLUDED DISCOUNT → base = 90 / 0.90 = 100 EUR
        assertEquals(
            10000L,
            addOnService.calculateIncludedBaseCost(
                totalAmountCents = 9000L,
                includedExactCents = 0L,
                totalIncludedPercentage = BigDecimal.ZERO,
                includedExactDiscountCents = 0L,
                totalIncludedDiscountPercentage = BigDecimal("10")
            )
        )
    }

    @Test
    fun `calculateIncludedBaseCost adds back EXACT INCLUDED DISCOUNT`() {
        // 90 EUR with 5 EUR EXACT INCLUDED DISCOUNT → base = 90 + 5 = 95 EUR
        assertEquals(
            9500L,
            addOnService.calculateIncludedBaseCost(
                totalAmountCents = 9000L,
                includedExactCents = 0L,
                totalIncludedPercentage = BigDecimal.ZERO,
                includedExactDiscountCents = 500L,
                totalIncludedDiscountPercentage = BigDecimal.ZERO
            )
        )
    }

    @Test
    fun `calculateIncludedBaseCost mixed non-discount tip and discount percentage`() {
        // 100 EUR with 10% INCLUDED TIP + 5% INCLUDED DISCOUNT
        // divisor = 1 + 0.10 − 0.05 = 1.05
        // base = 100 / 1.05 = 95.24 → 9524 cents
        assertEquals(
            9524L,
            addOnService.calculateIncludedBaseCost(
                totalAmountCents = 10000L,
                includedExactCents = 0L,
                totalIncludedPercentage = BigDecimal("10"),
                includedExactDiscountCents = 0L,
                totalIncludedDiscountPercentage = BigDecimal("5")
            )
        )
    }

    @Test
    fun `calculateIncludedBaseCost guards against 100 percent INCLUDED DISCOUNT`() {
        // 100% INCLUDED DISCOUNT → divisor = 1 − 1 = 0 → guarded
        assertEquals(
            8000L,
            addOnService.calculateIncludedBaseCost(
                totalAmountCents = 8000L,
                includedExactCents = 0L,
                totalIncludedPercentage = BigDecimal.ZERO,
                includedExactDiscountCents = 0L,
                totalIncludedDiscountPercentage = BigDecimal("100")
            )
        )
    }

    // ── resolveAddOnAmountCents ───────────────────────────────────────────

    @Nested
    inner class ResolveAddOnAmountCents {

        @Test
        fun `EXACT converts decimal amount to cents`() {
            // 6.21 EUR → 621 cents
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("6.21"),
                valueType = AddOnValueType.EXACT,
                decimalDigits = 2,
                sourceAmountCents = 0L
            )
            assertEquals(621L, result)
        }

        @Test
        fun `EXACT handles zero-decimal currency`() {
            // 100 JPY → 100 cents (decimalDigits=0)
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("100"),
                valueType = AddOnValueType.EXACT,
                decimalDigits = 0,
                sourceAmountCents = 0L
            )
            assertEquals(100L, result)
        }

        @Test
        fun `EXACT rounds half-up`() {
            // 1.005 with 2 decimal digits → 1.005 * 100 = 100.5 → rounds to 101
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("1.005"),
                valueType = AddOnValueType.EXACT,
                decimalDigits = 2,
                sourceAmountCents = 0L
            )
            assertEquals(101L, result)
        }

        @Test
        fun `PERCENTAGE computes cents from source amount`() {
            // 10% of 6831 cents = 683.1 → rounds to 683
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("10"),
                valueType = AddOnValueType.PERCENTAGE,
                decimalDigits = 2,
                sourceAmountCents = 6831L
            )
            assertEquals(683L, result)
        }

        @Test
        fun `PERCENTAGE returns zero when sourceAmountCents is zero`() {
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("15"),
                valueType = AddOnValueType.PERCENTAGE,
                decimalDigits = 2,
                sourceAmountCents = 0L
            )
            assertEquals(0L, result)
        }

        @Test
        fun `PERCENTAGE returns zero when sourceAmountCents is negative`() {
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("10"),
                valueType = AddOnValueType.PERCENTAGE,
                decimalDigits = 2,
                sourceAmountCents = -500L
            )
            assertEquals(0L, result)
        }

        @Test
        fun `PERCENTAGE 50 percent of 10000 cents`() {
            val result = addOnService.resolveAddOnAmountCents(
                normalizedInput = BigDecimal("50"),
                valueType = AddOnValueType.PERCENTAGE,
                decimalDigits = 2,
                sourceAmountCents = 10000L
            )
            assertEquals(5000L, result)
        }
    }

    // ── convertGroupToSourceCents ─────────────────────────────────────────

    @Nested
    inner class ConvertGroupToSourceCents {

        @Test
        fun `converts with exchange rate of 1`() {
            val result = addOnService.convertGroupToSourceCents(
                groupAmountCents = 5000L,
                exchangeRate = BigDecimal.ONE
            )
            assertEquals(5000L, result)
        }

        @Test
        fun `converts with fractional exchange rate`() {
            // 1000 / 0.027 = 37037.037... → rounds to 37037
            val result = addOnService.convertGroupToSourceCents(
                groupAmountCents = 1000L,
                exchangeRate = BigDecimal("0.027")
            )
            assertEquals(37037L, result)
        }

        @Test
        fun `converts with exchange rate greater than 1`() {
            // 5000 / 2.5 = 2000
            val result = addOnService.convertGroupToSourceCents(
                groupAmountCents = 5000L,
                exchangeRate = BigDecimal("2.5")
            )
            assertEquals(2000L, result)
        }

        @Test
        fun `returns groupAmountCents when exchangeRate is zero`() {
            val result = addOnService.convertGroupToSourceCents(
                groupAmountCents = 5000L,
                exchangeRate = BigDecimal.ZERO
            )
            assertEquals(5000L, result)
        }

        @Test
        fun `rounds half-up`() {
            // 100 / 3 = 33.333... → rounds to 33
            val result = addOnService.convertGroupToSourceCents(
                groupAmountCents = 100L,
                exchangeRate = BigDecimal("3")
            )
            assertEquals(33L, result)
        }
    }

    // ── sumPercentagesFromInputs ──────────────────────────────────────────

    @Nested
    inner class SumPercentagesFromInputs {

        @Test
        fun `sums valid percentage strings`() {
            val result = addOnService.sumPercentagesFromInputs(listOf("33.33", "33.33", "33.34"))
            assertEquals(BigDecimal("100.00"), result)
        }

        @Test
        fun `returns zero for empty list`() {
            assertEquals(BigDecimal.ZERO, addOnService.sumPercentagesFromInputs(emptyList()))
        }

        @Test
        fun `treats unparseable inputs as zero`() {
            val result = addOnService.sumPercentagesFromInputs(listOf("25", "abc", "25"))
            assertEquals(BigDecimal("50"), result)
        }

        @Test
        fun `handles blank and whitespace-only strings`() {
            val result = addOnService.sumPercentagesFromInputs(listOf("50", "  ", "50"))
            assertEquals(BigDecimal("100"), result)
        }

        @Test
        fun `handles single element`() {
            val result = addOnService.sumPercentagesFromInputs(listOf("42.5"))
            assertEquals(BigDecimal("42.5"), result)
        }
    }

    // ── calculateIncludedDiscountPercentageCents ─────────────────────────

    @Nested
    inner class CalculateIncludedDiscountPercentageCents {

        @Test
        fun `10 percent on 1821_52 EUR returns 202_39 EUR (20239 cents) - issue 1078 scenario`() {
            // baseCost = 182152 / 0.9 = 202391; discount = 202391 - 182152 = 20239
            // Equivalent: 182152 * 10 / 90 = 20239.111 → 20239 (HALF_UP)
            val result = addOnService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = 182152L,
                discountPercentage = BigDecimal("10")
            )
            assertEquals(20239L, result)
        }

        @Test
        fun `does NOT use the buggy source × pct ÷ 100 formula`() {
            // The buggy formula would return 18215 (10% of 1821.52).
            // The correct formula returns 20239 (the actual embedded discount).
            val buggy = 182152L * 10L / 100L
            val result = addOnService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = 182152L,
                discountPercentage = BigDecimal("10")
            )
            assertEquals(20239L, result)
            assertEquals(18215L, buggy)
            assertNotEquals(buggy, result)
        }

        @Test
        fun `25 percent on 7500 cents returns 2500 cents`() {
            // baseCost = 7500 / 0.75 = 10000; discount = 2500
            val result = addOnService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = 7500L,
                discountPercentage = BigDecimal("25")
            )
            assertEquals(2500L, result)
        }

        @Test
        fun `zero percent returns zero`() {
            val result = addOnService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = 100000L,
                discountPercentage = BigDecimal.ZERO
            )
            assertEquals(0L, result)
        }

        @Test
        fun `negative percent returns zero`() {
            val result = addOnService.calculateIncludedDiscountPercentageCents(
                sourceAmountCents = 100000L,
                discountPercentage = BigDecimal("-10")
            )
            assertEquals(0L, result)
        }

        @Test
        fun `100 percent or above returns zero (degenerate - no base cost)`() {
            assertEquals(
                0L,
                addOnService.calculateIncludedDiscountPercentageCents(
                    sourceAmountCents = 100000L,
                    discountPercentage = BigDecimal("100")
                )
            )
            assertEquals(
                0L,
                addOnService.calculateIncludedDiscountPercentageCents(
                    sourceAmountCents = 100000L,
                    discountPercentage = BigDecimal("150")
                )
            )
        }

        @Test
        fun `zero or negative source amount returns zero`() {
            assertEquals(
                0L,
                addOnService.calculateIncludedDiscountPercentageCents(
                    sourceAmountCents = 0L,
                    discountPercentage = BigDecimal("10")
                )
            )
            assertEquals(
                0L,
                addOnService.calculateIncludedDiscountPercentageCents(
                    sourceAmountCents = -100L,
                    discountPercentage = BigDecimal("10")
                )
            )
        }
    }
}
