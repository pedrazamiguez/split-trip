package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

class ExpenseCalculatorServiceTest {

    private val service = ExpenseCalculatorServiceImpl()

    // ── centsToBigDecimal ────────────────────────────────────────────────

    @Test
    fun `centsToBigDecimal converts cents to decimal for standard currency`() {
        val result = service.centsToBigDecimal(12345L)
        assertEquals(BigDecimal("123.45"), result)
    }

    @Test
    fun `centsToBigDecimal handles zero decimal places currency like JPY`() {
        val result = service.centsToBigDecimal(12345L, decimalPlaces = 0)
        assertEquals(BigDecimal("12345"), result)
    }

    @Test
    fun `centsToBigDecimal handles three decimal places currency like TND`() {
        val result = service.centsToBigDecimal(12345L, decimalPlaces = 3)
        assertEquals(BigDecimal("12.345"), result)
    }

    // ── FIFO Cash Calculation ────────────────────────────────────────────

    @Test
    fun `hasInsufficientCash returns false when sufficient cash available`() {
        val withdrawals = listOf(
            createWithdrawal(remainingAmount = 10000L)
        )
        assertFalse(service.hasInsufficientCash(5000L, withdrawals))
    }

    @Test
    fun `hasInsufficientCash returns true when insufficient cash`() {
        val withdrawals = listOf(
            createWithdrawal(remainingAmount = 3000L)
        )
        assertTrue(service.hasInsufficientCash(5000L, withdrawals))
    }

    @Test
    fun `hasInsufficientCash returns false when exact match`() {
        val withdrawals = listOf(
            createWithdrawal(remainingAmount = 5000L)
        )
        assertFalse(service.hasInsufficientCash(5000L, withdrawals))
    }

    @Test
    fun `calculateFifoCashAmount single withdrawal exact match`() {
        // 10000 THB withdrawn at rate: deductedBase=27000 EUR cents for 1000000 THB cents
        // rate = 27000/1000000 = 0.027
        val withdrawals = listOf(
            createWithdrawal(
                id = "w1",
                amountWithdrawn = 1000000L, // 10000 THB in cents
                remainingAmount = 1000000L,
                deductedBaseAmount = 27000L // 270 EUR in cents
            )
        )
        val result = service.calculateFifoCashAmount(1000000L, withdrawals)

        assertEquals(1, result.tranches.size)
        assertEquals("w1", result.tranches[0].withdrawalId)
        assertEquals(1000000L, result.tranches[0].amountConsumed)
        assertEquals(27000L, result.groupAmountCents)
    }

    @Test
    fun `calculateFifoCashAmount multi-withdrawal FIFO consumption`() {
        val w1 = createWithdrawal(
            id = "w1",
            amountWithdrawn = 1000000L,
            remainingAmount = 5000L,
            deductedBaseAmount = 26400L
        )
        val w2 = createWithdrawal(
            id = "w2",
            amountWithdrawn = 500000L,
            remainingAmount = 500000L,
            deductedBaseAmount = 13587L
        )

        val result = service.calculateFifoCashAmount(23000L, listOf(w1, w2))

        assertEquals(2, result.tranches.size)
        assertEquals("w1", result.tranches[0].withdrawalId)
        assertEquals(5000L, result.tranches[0].amountConsumed)
        assertEquals("w2", result.tranches[1].withdrawalId)
        assertEquals(18000L, result.tranches[1].amountConsumed)
        assertTrue(result.groupAmountCents > 0)
    }

    @Test
    fun `calculateFifoCashAmount single withdrawal partial consumption`() {
        val w1 = createWithdrawal(
            id = "w1",
            amountWithdrawn = 1000000L,
            remainingAmount = 1000000L,
            deductedBaseAmount = 27000L
        )

        val result = service.calculateFifoCashAmount(50000L, listOf(w1))

        assertEquals(1, result.tranches.size)
        assertEquals("w1", result.tranches[0].withdrawalId)
        assertEquals(50000L, result.tranches[0].amountConsumed)
        assertEquals(1350L, result.groupAmountCents)
    }

    @Test
    fun `calculateFifoCashAmount throws on insufficient cash`() {
        val withdrawals = listOf(
            createWithdrawal(remainingAmount = 3000L, amountWithdrawn = 3000L, deductedBaseAmount = 100L)
        )
        assertThrows<IllegalStateException> {
            service.calculateFifoCashAmount(5000L, withdrawals)
        }
    }

    @Test
    fun `calculateFifoCashAmount throws on zero amount`() {
        val withdrawals = listOf(
            createWithdrawal(remainingAmount = 5000L, amountWithdrawn = 5000L, deductedBaseAmount = 100L)
        )
        assertThrows<IllegalArgumentException> {
            service.calculateFifoCashAmount(0L, withdrawals)
        }
    }

    @Test
    fun `calculateFifoCashAmount same currency same rate`() {
        val w1 = createWithdrawal(
            id = "w1",
            amountWithdrawn = 50000L,
            remainingAmount = 50000L,
            deductedBaseAmount = 50000L
        )

        val result = service.calculateFifoCashAmount(30000L, listOf(w1))

        assertEquals(1, result.tranches.size)
        assertEquals(30000L, result.tranches[0].amountConsumed)
        assertEquals(30000L, result.groupAmountCents)
    }

    // ── distributeAmount ─────────────────────────────────────────────────

    data class DistributeTestCase(
        val description: String,
        val totalAmount: BigDecimal,
        val numberOfUsers: Int,
        val decimalPlaces: Int,
        val expectedAllocations: List<BigDecimal>
    ) {
        override fun toString(): String = description
    }

    companion object {
        @JvmStatic
        @Suppress("LongMethod") // Parameterized test data factory — length is data, not logic
        fun distributeAmountTestCases(): Stream<DistributeTestCase> = Stream.of(
            DistributeTestCase(
                description = "100 divided by 3 users (classic remainder)",
                totalAmount = BigDecimal("100.00"),
                numberOfUsers = 3,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("33.34"),
                    BigDecimal("33.33"),
                    BigDecimal("33.33")
                )
            ),
            DistributeTestCase(
                description = "10.00 divided by 2 users (even split)",
                totalAmount = BigDecimal("10.00"),
                numberOfUsers = 2,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("5.00"),
                    BigDecimal("5.00")
                )
            ),
            DistributeTestCase(
                description = "100.00 divided by 1 user (single user)",
                totalAmount = BigDecimal("100.00"),
                numberOfUsers = 1,
                decimalPlaces = 2,
                expectedAllocations = listOf(BigDecimal("100.00"))
            ),
            // Large group: 10.00 / 7 = floor 1.42, remainder 0.06 (6 cents to first 6 users)
            DistributeTestCase(
                description = "10.00 divided by 7 users (large group remainder)",
                totalAmount = BigDecimal("10.00"),
                numberOfUsers = 7,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("1.43"),
                    BigDecimal("1.43"),
                    BigDecimal("1.43"),
                    BigDecimal("1.43"),
                    BigDecimal("1.43"),
                    BigDecimal("1.43"),
                    BigDecimal("1.42")
                )
            ),
            DistributeTestCase(
                description = "1000 JPY divided by 3 users (zero decimals)",
                totalAmount = BigDecimal("1000"),
                numberOfUsers = 3,
                decimalPlaces = 0,
                expectedAllocations = listOf(
                    BigDecimal("334"),
                    BigDecimal("333"),
                    BigDecimal("333")
                )
            ),
            DistributeTestCase(
                description = "10.000 TND divided by 3 users (three decimals)",
                totalAmount = BigDecimal("10.000"),
                numberOfUsers = 3,
                decimalPlaces = 3,
                expectedAllocations = listOf(
                    BigDecimal("3.334"),
                    BigDecimal("3.333"),
                    BigDecimal("3.333")
                )
            ),
            DistributeTestCase(
                description = "0.01 divided by 3 users (minimum unit remainder)",
                totalAmount = BigDecimal("0.01"),
                numberOfUsers = 3,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("0.01"),
                    BigDecimal("0.00"),
                    BigDecimal("0.00")
                )
            ),
            DistributeTestCase(
                description = "0.05 divided by 3 users (2-cent remainder)",
                totalAmount = BigDecimal("0.05"),
                numberOfUsers = 3,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("0.02"),
                    BigDecimal("0.02"),
                    BigDecimal("0.01")
                )
            ),
            // Edge case: totalAmount has more fractional digits than decimalPlaces
            // 10.005 normalized to 2 decimals = 10.01 (HALF_UP), then split among 3
            DistributeTestCase(
                description = "10.005 divided by 3 users (excess precision normalized)",
                totalAmount = BigDecimal("10.005"),
                numberOfUsers = 3,
                decimalPlaces = 2,
                expectedAllocations = listOf(
                    BigDecimal("3.34"),
                    BigDecimal("3.34"),
                    BigDecimal("3.33")
                )
            )
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("distributeAmountTestCases")
    fun `distributeAmount allocates correctly and conserves total`(testCase: DistributeTestCase) {
        val result = service.distributeAmount(
            totalAmount = testCase.totalAmount,
            numberOfUsers = testCase.numberOfUsers,
            decimalPlaces = testCase.decimalPlaces
        )

        assertEquals(
            testCase.expectedAllocations.size,
            result.size,
            "Expected ${testCase.expectedAllocations.size} allocations but got ${result.size}"
        )
        testCase.expectedAllocations.forEachIndexed { index, expected ->
            assertEquals(
                expected,
                result[index],
                "Allocation at index $index: expected $expected but got ${result[index]}"
            )
        }

        // Conservation invariant: sum of allocations must equal the normalized total
        // (totalAmount rounded to decimalPlaces, since the method normalizes internally)
        val normalizedTotal = testCase.totalAmount.setScale(testCase.decimalPlaces, java.math.RoundingMode.HALF_UP)
        val sum = result.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
        assertEquals(
            0,
            normalizedTotal.compareTo(sum),
            "Sum of allocations ($sum) must equal normalized total ($normalizedTotal)"
        )
    }

    @ParameterizedTest(name = "Total={0}, Users={1}")
    @CsvSource(
        "100.00, 3",
        "99.99, 7",
        "1.00, 11",
        "0.01, 2",
        "10000.00, 13",
        "1.00, 100"
    )
    fun `distributeAmount sum always equals total (conservation invariant)`(totalStr: String, numberOfUsers: Int) {
        val total = BigDecimal(totalStr)
        val result = service.distributeAmount(total, numberOfUsers)

        val sum = result.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
        assertEquals(
            0,
            total.compareTo(sum),
            "Conservation violated: sum=$sum != total=$total for $numberOfUsers users"
        )
        assertEquals(numberOfUsers, result.size)
    }

    @Test
    fun `distributeAmount throws on zero users`() {
        assertThrows<IllegalArgumentException> {
            service.distributeAmount(BigDecimal("100.00"), 0)
        }
    }

    @Test
    fun `distributeAmount throws on negative users`() {
        assertThrows<IllegalArgumentException> {
            service.distributeAmount(BigDecimal("100.00"), -1)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun createWithdrawal(
        id: String = "w-test",
        amountWithdrawn: Long = 1000000L,
        remainingAmount: Long = 1000000L,
        deductedBaseAmount: Long = 27000L,
        currency: String = "THB"
    ) = CashWithdrawal(
        id = id,
        groupId = "group-1",
        withdrawnBy = "user-1",
        amountWithdrawn = amountWithdrawn,
        remainingAmount = remainingAmount,
        currency = currency,
        deductedBaseAmount = deductedBaseAmount,
        exchangeRate = if (deductedBaseAmount > 0) {
            BigDecimal(amountWithdrawn).divide(BigDecimal(deductedBaseAmount), 6, java.math.RoundingMode.HALF_UP)
        } else {
            BigDecimal.ONE
        },
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
    )

    // ── computeProportionalAmount ────────────────────────────────────────

    @Nested
    inner class ComputeProportionalAmount {

        @Test
        fun `scales proportionally`() {
            // 5000 * 4000 / 10000 = 2000
            val result = service.computeProportionalAmount(
                amount = 5000L,
                targetAmount = 4000L,
                totalAmount = 10000L
            )
            assertEquals(2000L, result)
        }

        @Test
        fun `returns targetAmount when amount equals totalAmount`() {
            val result = service.computeProportionalAmount(
                amount = 10000L,
                targetAmount = 8000L,
                totalAmount = 10000L
            )
            assertEquals(8000L, result)
        }

        @Test
        fun `returns targetAmount when totalAmount is zero`() {
            val result = service.computeProportionalAmount(
                amount = 5000L,
                targetAmount = 3000L,
                totalAmount = 0L
            )
            assertEquals(3000L, result)
        }

        @Test
        fun `rounds half-up`() {
            // 3333 * 5000 / 10000 = 1666.5 → rounds to 1667
            val result = service.computeProportionalAmount(
                amount = 3333L,
                targetAmount = 5000L,
                totalAmount = 10000L
            )
            assertEquals(1667L, result)
        }

        @Test
        fun `handles single cent amount`() {
            // 1 * 5000 / 10000 = 0.5 → rounds to 1
            val result = service.computeProportionalAmount(
                amount = 1L,
                targetAmount = 5000L,
                totalAmount = 10000L
            )
            assertEquals(1L, result)
        }
    }

    // ── centsToBigDecimalString ───────────────────────────────────────────

    @Nested
    inner class CentsToBigDecimalString {

        @Test
        fun `converts 1550 cents with 2 decimal places to 15_50`() {
            assertEquals("15.50", service.centsToBigDecimalString(1550L, 2))
        }

        @Test
        fun `converts 0 cents to 0_00`() {
            assertEquals("0.00", service.centsToBigDecimalString(0L, 2))
        }

        @Test
        fun `converts 500 cents with 0 decimal places (JPY) to 500`() {
            assertEquals("500", service.centsToBigDecimalString(500L, 0))
        }

        @Test
        fun `converts 1234 cents with 3 decimal places (TND) to 1_234`() {
            assertEquals("1.234", service.centsToBigDecimalString(1234L, 3))
        }

        @Test
        fun `uses default 2 decimal places when not specified`() {
            assertEquals("25.00", service.centsToBigDecimalString(2500L))
        }
    }
}
