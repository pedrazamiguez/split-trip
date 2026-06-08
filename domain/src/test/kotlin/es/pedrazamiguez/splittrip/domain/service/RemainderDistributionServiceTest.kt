package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.service.impl.RemainderDistributionServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RemainderDistributionServiceTest {

    private val service = RemainderDistributionServiceImpl()

    // ── distributeByWeights ─────────────────────────────────────────────

    @Nested
    inner class DistributeByWeights {

        @Test
        fun `distributes evenly when weights are equal`() {
            val result = service.distributeByWeights(9000L, listOf(BigDecimal.ONE, BigDecimal.ONE))
            assertEquals(listOf(4500L, 4500L), result)
        }

        @Test
        fun `distributes proportionally with different weights`() {
            val result = service.distributeByWeights(
                100L,
                listOf(BigDecimal("30"), BigDecimal("70"))
            )
            assertEquals(listOf(30L, 70L), result)
        }

        @Test
        fun `distributes remainder to first items when not evenly divisible`() {
            val result = service.distributeByWeights(
                10L,
                listOf(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE)
            )
            // 10 / 3 = 3 each with 1 remainder → first gets +1
            assertEquals(listOf(4L, 3L, 3L), result)
            assertEquals(10L, result.sum())
        }

        @Test
        fun `distributes remainder across multiple items`() {
            val result = service.distributeByWeights(
                10L,
                listOf(
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE,
                    BigDecimal.ONE
                )
            )
            // 10 / 7 = 1 each (floor), remainder = 3 → first 3 get +1
            assertEquals(listOf(2L, 2L, 2L, 1L, 1L, 1L, 1L), result)
            assertEquals(10L, result.sum())
        }

        @Test
        fun `returns empty list for empty weights`() {
            val result = service.distributeByWeights(100L, emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns zeros when total is zero`() {
            val result = service.distributeByWeights(
                0L,
                listOf(BigDecimal.ONE, BigDecimal.ONE)
            )
            assertEquals(listOf(0L, 0L), result)
        }

        @Test
        fun `returns zeros when all weights are zero`() {
            val result = service.distributeByWeights(
                100L,
                listOf(BigDecimal.ZERO, BigDecimal.ZERO)
            )
            assertEquals(listOf(0L, 0L), result)
        }

        @Test
        fun `handles single item`() {
            val result = service.distributeByWeights(
                4567L,
                listOf(BigDecimal.ONE)
            )
            assertEquals(listOf(4567L), result)
        }

        @Test
        fun `sum always equals total for asymmetric weights`() {
            val result = service.distributeByWeights(
                10000L,
                listOf(BigDecimal("1"), BigDecimal("2"), BigDecimal("3"))
            )
            assertEquals(10000L, result.sum())
        }

        @Test
        fun `returns zeros when total is negative`() {
            val result = service.distributeByWeights(
                -100L,
                listOf(BigDecimal.ONE, BigDecimal.ONE)
            )
            assertEquals(listOf(0L, 0L), result)
        }
    }

    // ── rescaleAmounts ──────────────────────────────────────────────────

    @Nested
    inner class RescaleAmounts {

        @Test
        fun `rescales 2 equal splits proportionally`() {
            // 10000 → 9000, 2 splits of 5000 each → 4500 each
            val result = service.rescaleAmounts(
                originalTotal = 10000L,
                newTotal = 9000L,
                amounts = listOf(5000L, 5000L)
            )
            assertEquals(listOf(4500L, 4500L), result)
            assertEquals(9000L, result.sum())
        }

        @Test
        fun `rescales 3 splits with remainder`() {
            // 1000 → 900, 3 splits of 334, 333, 333
            val result = service.rescaleAmounts(
                originalTotal = 1000L,
                newTotal = 900L,
                amounts = listOf(334L, 333L, 333L)
            )
            assertEquals(900L, result.sum())
        }

        @Test
        fun `returns same amounts when originalTotal equals newTotal`() {
            val amounts = listOf(100L, 200L, 300L)
            val result = service.rescaleAmounts(
                originalTotal = 600L,
                newTotal = 600L,
                amounts = amounts
            )
            assertEquals(amounts, result)
        }

        @Test
        fun `returns same amounts when originalTotal is zero`() {
            val amounts = listOf(100L, 200L)
            val result = service.rescaleAmounts(
                originalTotal = 0L,
                newTotal = 500L,
                amounts = amounts
            )
            assertEquals(amounts, result)
        }

        @Test
        fun `returns empty list when amounts are empty`() {
            val result = service.rescaleAmounts(
                originalTotal = 1000L,
                newTotal = 500L,
                amounts = emptyList()
            )
            assertTrue(result.isEmpty())
        }

        @Test
        fun `skips excluded items for remainder distribution`() {
            // 1000 → 899, 3 splits → base scale is 0.899
            // 334 * 0.899 = 300 (floor), 333 * 0.899 = 299 (floor), 333 * 0.899 = 299 (floor)
            // sum = 898, remainder = 1
            // item[0] is excluded → skip, item[1] gets the +1
            val result = service.rescaleAmounts(
                originalTotal = 1000L,
                newTotal = 899L,
                amounts = listOf(334L, 333L, 333L),
                isExcluded = listOf(true, false, false)
            )
            assertEquals(899L, result.sum())
            // First item (excluded) should NOT get the remainder
            assertEquals(300L, result[0])
        }

        @Test
        fun `handles scale-down to very small amounts`() {
            val result = service.rescaleAmounts(
                originalTotal = 10000L,
                newTotal = 1L,
                amounts = listOf(5000L, 5000L)
            )
            assertEquals(1L, result.sum())
        }
    }

    // ── distributePercentages ───────────────────────────────────────────

    @Nested
    inner class DistributePercentages {

        @Test
        fun `distributes 100 percent evenly across 2 equal splits`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = listOf(5000L, 5000L),
                totalCents = 10000L
            )
            assertEquals(2, result.size)
            assertEquals(BigDecimal("50.00"), result[0])
            assertEquals(BigDecimal("50.00"), result[1])
            assertEquals(
                0,
                BigDecimal("100.00").compareTo(result.fold(BigDecimal.ZERO, BigDecimal::add))
            )
        }

        @Test
        fun `distributes 100 percent across 3 equal splits with remainder`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = listOf(3334L, 3333L, 3333L),
                totalCents = 10000L
            )
            assertEquals(3, result.size)
            // Sum must equal 100.00 exactly
            assertEquals(
                0,
                BigDecimal("100.00").compareTo(result.fold(BigDecimal.ZERO, BigDecimal::add))
            )
        }

        @Test
        fun `distributes partial remaining percentage`() {
            // If 40% is already claimed, distribute the remaining 60%
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("60.00"),
                amounts = listOf(3000L, 3000L),
                totalCents = 6000L
            )
            assertEquals(2, result.size)
            assertEquals(
                0,
                BigDecimal("60.00").compareTo(result.fold(BigDecimal.ZERO, BigDecimal::add))
            )
        }

        @Test
        fun `distributes proportionally with different amounts`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = listOf(7000L, 3000L),
                totalCents = 10000L
            )
            assertEquals(BigDecimal("70.00"), result[0])
            assertEquals(BigDecimal("30.00"), result[1])
        }

        @Test
        fun `returns empty list for empty amounts`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = emptyList(),
                totalCents = 10000L
            )
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when totalCents is zero`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = listOf(100L, 200L),
                totalCents = 0L
            )
            assertTrue(result.isEmpty())
        }

        @Test
        fun `handles single split - receives full percentage`() {
            val result = service.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = listOf(5000L),
                totalCents = 5000L
            )
            assertEquals(1, result.size)
            assertEquals(0, BigDecimal("100.00").compareTo(result[0]))
        }
    }
}
