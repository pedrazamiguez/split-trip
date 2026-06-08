package es.pedrazamiguez.splittrip.domain.service.split

import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SplitPreviewServiceTest {

    private lateinit var service: SplitPreviewService

    @BeforeEach
    fun setUp() {
        service = SplitPreviewServiceImpl()
    }

    // ── distributePercentagesEvenly ──────────────────────────────────────

    @Nested
    inner class DistributePercentagesEvenly {

        @Test
        fun `distributes 100 percent evenly among 2 participants`() {
            val shares = service.distributePercentagesEvenly(10000L, listOf("user1", "user2"))

            assertEquals(2, shares.size)
            assertEquals(BigDecimal("50.00"), shares[0].percentage)
            assertEquals(BigDecimal("50.00"), shares[1].percentage)
            assertEquals(5000L, shares[0].amountCents)
            assertEquals(5000L, shares[1].amountCents)
        }

        @Test
        fun `distributes 100 percent evenly among 3 participants with remainder`() {
            val shares =
                service.distributePercentagesEvenly(10000L, listOf("user1", "user2", "user3"))

            assertEquals(3, shares.size)
            // 100 / 3 = 33.33 with 0.01 remainder → first gets 33.34
            assertEquals(BigDecimal("33.34"), shares[0].percentage)
            assertEquals(BigDecimal("33.33"), shares[1].percentage)
            assertEquals(BigDecimal("33.33"), shares[2].percentage)
            // Percentages sum to 100.00
            val totalPct = shares.mapNotNull { it.percentage }.fold(BigDecimal.ZERO) { acc, p -> acc.add(p) }
            assertEquals(BigDecimal("100.00"), totalPct)
        }

        @Test
        fun `single participant gets 100 percent`() {
            val shares = service.distributePercentagesEvenly(5000L, listOf("user1"))

            assertEquals(1, shares.size)
            assertEquals(BigDecimal("100.00"), shares[0].percentage)
            assertEquals(5000L, shares[0].amountCents)
        }

        @Test
        fun `returns empty list for empty participants`() {
            val shares = service.distributePercentagesEvenly(10000L, emptyList())
            assertEquals(0, shares.size)
        }

        @Test
        fun `computes correct amount cents for 3-way split of 1000 cents`() {
            val shares =
                service.distributePercentagesEvenly(1000L, listOf("user1", "user2", "user3"))

            // 33.34% of 1000 = 333 (DOWN), 33.33% of 1000 = 333 (DOWN)
            // Remainder = 1000 - 999 = 1 → first participant gets the extra cent
            assertEquals(334L, shares[0].amountCents)
            assertEquals(333L, shares[1].amountCents)
            assertEquals(333L, shares[2].amountCents)
            // Amount conservation invariant: sum must equal the total
            assertEquals(1000L, shares.sumOf { it.amountCents })
        }

        @Test
        fun `zero source amount results in zero amount cents`() {
            val shares = service.distributePercentagesEvenly(0L, listOf("user1", "user2"))

            assertEquals(2, shares.size)
            assertEquals(0L, shares[0].amountCents)
            assertEquals(0L, shares[1].amountCents)
            // Percentages should still be distributed
            assertEquals(BigDecimal("50.00"), shares[0].percentage)
            assertEquals(BigDecimal("50.00"), shares[1].percentage)
        }

        @Test
        fun `returns user IDs in sorted order`() {
            val ids = listOf("alice", "bob", "charlie")
            val shares = service.distributePercentagesEvenly(9000L, ids)

            assertEquals("alice", shares[0].userId)
            assertEquals("bob", shares[1].userId)
            assertEquals("charlie", shares[2].userId)
        }

        @Test
        fun `remainder allocation is deterministic regardless of input order`() {
            val sharesAsc = service.distributePercentagesEvenly(10000L, listOf("alice", "bob", "charlie"))
            val sharesDesc = service.distributePercentagesEvenly(10000L, listOf("charlie", "bob", "alice"))

            val mapAsc = sharesAsc.associate { it.userId to it.percentage }
            val mapDesc = sharesDesc.associate { it.userId to it.percentage }
            assertEquals(mapAsc, mapDesc)
            // "alice" gets the extra 0.01% (sorted first)
            assertEquals(BigDecimal("33.34"), mapAsc["alice"])
            assertEquals(BigDecimal("33.33"), mapAsc["bob"])
            assertEquals(BigDecimal("33.33"), mapAsc["charlie"])
        }

        @Test
        fun `distributes among 7 participants with correct remainder`() {
            val shares = service.distributePercentagesEvenly(
                10000L,
                listOf("u1", "u2", "u3", "u4", "u5", "u6", "u7")
            )

            // 100 / 7 = 14.28 (DOWN to 2dp), 14.28 * 7 = 99.96, remainder = 0.04 → 4 extra units
            assertEquals(7, shares.size)
            val totalPct = shares.mapNotNull { it.percentage }.fold(BigDecimal.ZERO) { acc, p -> acc.add(p) }
            assertEquals(0, BigDecimal("100.00").compareTo(totalPct))
            // Amount conservation invariant
            assertEquals(10000L, shares.sumOf { it.amountCents })
        }

        @Test
        fun `50-50 split of odd cent amount distributes remainder (issue 455)`() {
            // Exact scenario from issue #455: €12.63 split 50-50
            val shares = service.distributePercentagesEvenly(1263L, listOf("user1", "user2"))

            assertEquals(2, shares.size)
            assertEquals(BigDecimal("50.00"), shares[0].percentage)
            assertEquals(BigDecimal("50.00"), shares[1].percentage)
            // 1263 * 50 / 100 = 631.5 → 631 (DOWN) each, remainder = 1
            // First user gets the extra cent
            assertEquals(632L, shares[0].amountCents)
            assertEquals(631L, shares[1].amountCents)
            // Amount conservation invariant
            assertEquals(1263L, shares.sumOf { it.amountCents })
        }

        @Test
        fun `amount conservation holds for various odd amounts`() {
            val oddAmounts = listOf(1L, 3L, 7L, 99L, 101L, 997L, 1001L, 9999L)
            val participantCounts = listOf(2, 3, 4, 5, 7)

            for (amount in oddAmounts) {
                for (count in participantCounts) {
                    val ids = (1..count).map { "u$it" }
                    val shares = service.distributePercentagesEvenly(amount, ids)
                    assertEquals(
                        amount,
                        shares.sumOf { it.amountCents },
                        "Amount conservation violated for $amount cents among $count participants"
                    )
                }
            }
        }
    }

    // ── redistributeRemainingPercentage ─────────────────────────────────

    @Nested
    inner class RedistributeRemainingPercentage {

        @Test
        fun `user typed 60 percent with 2 others distributes 20 percent each`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("60"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3")
            )

            assertEquals(2, shares.size)
            assertEquals(BigDecimal("20.00"), shares[0].percentage)
            assertEquals(BigDecimal("20.00"), shares[1].percentage)
            assertEquals(2000L, shares[0].amountCents)
            assertEquals(2000L, shares[1].amountCents)
        }

        @Test
        fun `user typed 0 percent distributes 100 percent among 3 others`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal.ZERO,
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3", "user4")
            )

            assertEquals(3, shares.size)
            // 100 / 3 = 33.33, remainder → first gets 33.34
            assertEquals(BigDecimal("33.34"), shares[0].percentage)
            assertEquals(BigDecimal("33.33"), shares[1].percentage)
            assertEquals(BigDecimal("33.33"), shares[2].percentage)
        }

        @Test
        fun `user typed 100 percent gives 0 percent to others`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("100"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3")
            )

            assertEquals(2, shares.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(shares[0].percentage))
            assertEquals(0, BigDecimal.ZERO.compareTo(shares[1].percentage))
            assertEquals(0L, shares[0].amountCents)
            assertEquals(0L, shares[1].amountCents)
        }

        @Test
        fun `user typed more than 100 percent clamps remaining to zero`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("120"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2")
            )

            assertEquals(1, shares.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(shares[0].percentage))
            assertEquals(0L, shares[0].amountCents)
        }

        @Test
        fun `single other participant gets all remaining`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("25"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2")
            )

            assertEquals(1, shares.size)
            assertEquals(BigDecimal("75.00"), shares[0].percentage)
            assertEquals(7500L, shares[0].amountCents)
        }

        @Test
        fun `returns empty list for empty other participants`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("50"),
                sourceAmountCents = 10000L,
                otherParticipantIds = emptyList()
            )

            assertEquals(0, shares.size)
        }

        @Test
        fun `zero source amount results in zero amount cents`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("40"),
                sourceAmountCents = 0L,
                otherParticipantIds = listOf("user2", "user3")
            )

            assertEquals(2, shares.size)
            assertEquals(0L, shares[0].amountCents)
            assertEquals(0L, shares[1].amountCents)
            // Percentages should still be distributed
            assertEquals(BigDecimal("30.00"), shares[0].percentage)
            assertEquals(BigDecimal("30.00"), shares[1].percentage)
        }

        @Test
        fun `amount conservation holds for 60-40 redistribution of odd amount`() {
            // User typed 60% of 1263 cents → remaining 40% for 1 other
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("60"),
                sourceAmountCents = 1263L,
                otherParticipantIds = listOf("user2")
            )

            assertEquals(1, shares.size)
            // Edited user: 1263 * 60 / 100 = 757 (DOWN)
            // Remaining for others: 1263 - 757 = 506
            assertEquals(506L, shares[0].amountCents)
        }

        @Test
        fun `amount conservation holds for 50-50 redistribution among 2 others (issue 455)`() {
            // User typed 50% of 1263 → remaining 50% split among 1 other
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("50"),
                sourceAmountCents = 1263L,
                otherParticipantIds = listOf("user2")
            )

            assertEquals(1, shares.size)
            // Edited user: 1263 * 50 / 100 = 631 (DOWN)
            // Remaining for other: 1263 - 631 = 632
            assertEquals(632L, shares[0].amountCents)
        }

        @Test
        fun `amount conservation for redistribution among 3 others with odd amount`() {
            val editedPct = BigDecimal("25")
            val sourceAmount = 1001L
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = editedPct,
                sourceAmountCents = sourceAmount,
                otherParticipantIds = listOf("u2", "u3", "u4")
            )

            val editedAmount = service.calculateAmountFromPercentage(editedPct, sourceAmount)
            val expectedRemaining = sourceAmount - editedAmount

            assertEquals(
                expectedRemaining,
                shares.sumOf { it.amountCents },
                "Other shares must sum to total minus edited user's share"
            )
        }
    }

    // ── redistributeRemainingPercentage with lockedPercentages ────────────

    @Nested
    inner class RedistributeRemainingPercentageWithLocks {

        @Test
        fun `locked member keeps percentage and only unlocked members get redistributed`() {
            // A=30%, B locked at 50%, C should get 20%
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("30"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3"),
                lockedPercentages = mapOf("user2" to BigDecimal("50"))
            )

            // Only user3 is unlocked
            assertEquals(1, shares.size)
            assertEquals("user3", shares[0].userId)
            assertEquals(BigDecimal("20.00"), shares[0].percentage)
            assertEquals(2000L, shares[0].amountCents)
        }

        @Test
        fun `30 50 20 scenario end-to-end`() {
            // Step 1: Type 30% for A → B and C redistribute (no locks)
            val step1 = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("30"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3")
            )
            assertEquals(BigDecimal("35.00"), step1[0].percentage)
            assertEquals(BigDecimal("35.00"), step1[1].percentage)

            // Step 2: A locked at 30%, type 50% for B → only C adjusts
            val step2 = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("50"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user1", "user3"),
                lockedPercentages = mapOf("user1" to BigDecimal("30"))
            )
            // user1 is locked (filtered out), only user3 returned
            assertEquals(1, step2.size)
            assertEquals("user3", step2[0].userId)
            assertEquals(BigDecimal("20.00"), step2[0].percentage)
            assertEquals(2000L, step2[0].amountCents)
        }

        @Test
        fun `all others locked returns empty list`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("30"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3"),
                lockedPercentages = mapOf(
                    "user2" to BigDecimal("40"),
                    "user3" to BigDecimal("30")
                )
            )

            assertEquals(0, shares.size)
        }

        @Test
        fun `locked shares exceeding budget clamps remainder to zero`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("50"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3"),
                lockedPercentages = mapOf("user2" to BigDecimal("60"))
            )

            // remaining = max(0, 100 - 50 - 60) = 0 → user3 gets 0%
            assertEquals(1, shares.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(shares[0].percentage))
            assertEquals(0L, shares[0].amountCents)
        }

        @Test
        fun `empty lockedPercentages is backward compatible`() {
            val shares = service.redistributeRemainingPercentage(
                editedPercentage = BigDecimal("60"),
                sourceAmountCents = 10000L,
                otherParticipantIds = listOf("user2", "user3"),
                lockedPercentages = emptyMap()
            )

            assertEquals(2, shares.size)
            assertEquals(BigDecimal("20.00"), shares[0].percentage)
            assertEquals(BigDecimal("20.00"), shares[1].percentage)
        }

        @Test
        fun `amount conservation with locks and odd amount`() {
            val editedPct = BigDecimal("30")
            val lockedPct = BigDecimal("25")
            val sourceAmount = 1263L

            val shares = service.redistributeRemainingPercentage(
                editedPercentage = editedPct,
                sourceAmountCents = sourceAmount,
                otherParticipantIds = listOf("user2", "user3", "user4"),
                lockedPercentages = mapOf("user2" to lockedPct)
            )

            // Only user3 and user4 are returned (unlocked)
            assertEquals(2, shares.size)
            // Verify total coherence
            val editedCents = service.calculateAmountFromPercentage(editedPct, sourceAmount)
            val lockedCents = service.calculateAmountFromPercentage(lockedPct, sourceAmount)
            val expectedRemaining = sourceAmount - editedCents - lockedCents
            assertEquals(
                expectedRemaining,
                shares.sumOf { it.amountCents },
                "Unlocked shares must sum to total minus edited and locked amounts"
            )
        }
    }

    // ── calculateAmountFromPercentage ────────────────────────────────────

    @Nested
    inner class CalculateAmountFromPercentage {

        @Test
        fun `50 pct of 10000 cents is 5000`() {
            val result = service.calculateAmountFromPercentage(BigDecimal("50"), 10000L)
            assertEquals(5000L, result)
        }

        @Test
        fun `33_33 pct of 10000 cents rounds down`() {
            val result = service.calculateAmountFromPercentage(BigDecimal("33.33"), 10000L)
            // 10000 * 33.33 / 100 = 3333.0 → 3333
            assertEquals(3333L, result)
        }

        @Test
        fun `100 pct of amount returns full amount`() {
            val result = service.calculateAmountFromPercentage(BigDecimal("100"), 7531L)
            assertEquals(7531L, result)
        }

        @Test
        fun `0 pct returns 0`() {
            val result = service.calculateAmountFromPercentage(BigDecimal.ZERO, 10000L)
            assertEquals(0L, result)
        }

        @Test
        fun `zero source amount returns 0`() {
            val result = service.calculateAmountFromPercentage(BigDecimal("50"), 0L)
            assertEquals(0L, result)
        }

        @Test
        fun `negative source amount returns 0`() {
            val result = service.calculateAmountFromPercentage(BigDecimal("50"), -100L)
            assertEquals(0L, result)
        }
    }

    // ── parseAmountToCents ──────────────────────────────────────────────

    @Nested
    inner class ParseAmountToCents {

        @Test
        fun `parses dot-decimal EUR amount`() {
            val result = service.parseAmountToCents("10.50", decimalDigits = 2)
            assertEquals(1050L, result)
        }

        @Test
        fun `parses comma-decimal EUR amount`() {
            val result = service.parseAmountToCents("10,50", decimalDigits = 2)
            assertEquals(1050L, result)
        }

        @Test
        fun `parses JPY amount with 0 decimal digits`() {
            val result = service.parseAmountToCents("1500", decimalDigits = 0)
            assertEquals(1500L, result)
        }

        @Test
        fun `parses TND amount with 3 decimal digits`() {
            val result = service.parseAmountToCents("10.500", decimalDigits = 3)
            assertEquals(10500L, result)
        }

        @Test
        fun `returns 0 for blank input`() {
            val result = service.parseAmountToCents("", decimalDigits = 2)
            assertEquals(0L, result)
        }

        @Test
        fun `returns 0 for invalid input`() {
            val result = service.parseAmountToCents("abc", decimalDigits = 2)
            assertEquals(0L, result)
        }

        @Test
        fun `handles thousand separators with dot-decimal`() {
            val result = service.parseAmountToCents("1,245.56", decimalDigits = 2)
            assertEquals(124556L, result)
        }

        @Test
        fun `handles thousand separators with comma-decimal`() {
            val result = service.parseAmountToCents("1.245,56", decimalDigits = 2)
            assertEquals(124556L, result)
        }

        @Test
        fun `uses default 2 decimal digits when not specified`() {
            val result = service.parseAmountToCents("25.99")
            assertEquals(2599L, result)
        }

        @Test
        fun `trims whitespace before parsing`() {
            val result = service.parseAmountToCents("  10.50  ", decimalDigits = 2)
            assertEquals(1050L, result)
        }
    }

    // ── parseToDecimal ──────────────────────────────────────────────────

    @Nested
    inner class ParseToDecimal {

        @Test
        fun `parses dot-decimal string`() {
            val result = service.parseToDecimal("33.33")
            assertEquals(0, BigDecimal("33.33").compareTo(result))
        }

        @Test
        fun `parses comma-decimal string`() {
            val result = service.parseToDecimal("33,33")
            assertEquals(0, BigDecimal("33.33").compareTo(result))
        }

        @Test
        fun `returns zero for blank input`() {
            val result = service.parseToDecimal("")
            assertEquals(0, BigDecimal.ZERO.compareTo(result))
        }

        @Test
        fun `returns zero for invalid input`() {
            val result = service.parseToDecimal("abc")
            assertEquals(0, BigDecimal.ZERO.compareTo(result))
        }

        @Test
        fun `trims whitespace before parsing`() {
            val result = service.parseToDecimal("  50.5  ")
            assertEquals(0, BigDecimal("50.5").compareTo(result))
        }

        @Test
        fun `parses integer string`() {
            val result = service.parseToDecimal("100")
            assertEquals(0, BigDecimal("100").compareTo(result))
        }
    }
}
