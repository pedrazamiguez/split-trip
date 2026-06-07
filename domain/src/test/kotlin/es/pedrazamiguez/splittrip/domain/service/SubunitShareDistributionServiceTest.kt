package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.service.impl.SubunitShareDistributionServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubunitShareDistributionService")
class SubunitShareDistributionServiceTest {

    private lateinit var service: SubunitShareDistributionService

    @BeforeEach
    fun setUp() {
        service = SubunitShareDistributionServiceImpl()
    }

    @Nested
    @DisplayName("distributeEvenly")
    inner class DistributeEvenly {

        @Test
        fun `distributes evenly among 2 members`() {
            val result = service.distributeEvenly(listOf("user-1", "user-2"))

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal("0.5").compareTo(result["user-1"]))
            assertEquals(0, BigDecimal("0.5").compareTo(result["user-2"]))
        }

        @Test
        fun `distributes evenly among 3 members`() {
            val result = service.distributeEvenly(listOf("user-1", "user-2", "user-3"))

            assertEquals(3, result.size)
            val expectedShare = BigDecimal.ONE.divide(BigDecimal(3), 10, java.math.RoundingMode.DOWN)
            result.values.forEach { share ->
                assertEquals(0, expectedShare.compareTo(share))
            }
            val sum = result.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
            assertTrue(sum.subtract(BigDecimal.ONE).abs() < BigDecimal("0.0001"))
        }

        @Test
        fun `single member gets 100 percent`() {
            val result = service.distributeEvenly(listOf("user-1"))

            assertEquals(1, result.size)
            assertEquals(0, BigDecimal.ONE.compareTo(result["user-1"]))
        }

        @Test
        fun `empty list returns empty map`() {
            val result = service.distributeEvenly(emptyList())

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("redistributeRemaining")
    inner class RedistributeRemaining {

        @Test
        fun `redistributes 40 percent among 2 others when one has 60 percent`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.6"),
                otherMemberIds = listOf("user-2", "user-3")
            )

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-2"]))
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-3"]))
        }

        @Test
        fun `redistributes 50 percent to single other`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.5"),
                otherMemberIds = listOf("user-2")
            )

            assertEquals(1, result.size)
            assertEquals(0, BigDecimal("0.5").compareTo(result["user-2"]))
        }

        @Test
        fun `edited share of 100 percent gives 0 to others`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal.ONE,
                otherMemberIds = listOf("user-2", "user-3")
            )

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(result["user-2"]))
            assertEquals(0, BigDecimal.ZERO.compareTo(result["user-3"]))
        }

        @Test
        fun `edited share over 100 percent clamps to 0 for others`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("1.5"),
                otherMemberIds = listOf("user-2")
            )

            assertEquals(1, result.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(result["user-2"]))
        }

        @Test
        fun `empty other members returns empty map`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.5"),
                otherMemberIds = emptyList()
            )

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("redistributeRemaining with lockedShares")
    inner class RedistributeRemainingWithLocks {

        @Test
        fun `locked member keeps value and only unlocked members are redistributed`() {
            // A=30%, B is locked at 50%, C should get 20%
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.3"),
                otherMemberIds = listOf("user-2", "user-3"),
                lockedShares = mapOf("user-2" to BigDecimal("0.5"))
            )

            // Only user-3 is unlocked — gets the remainder (1 - 0.3 - 0.5 = 0.2)
            assertEquals(1, result.size)
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-3"]))
        }

        @Test
        fun `30 50 20 scenario works correctly across two edits`() {
            // Step 1: Type 30% for A → redistribute among B and C (no locks yet)
            val step1 = service.redistributeRemaining(
                editedShare = BigDecimal("0.3"),
                otherMemberIds = listOf("user-2", "user-3")
            )
            assertEquals(0, BigDecimal("0.35").compareTo(step1["user-2"]))
            assertEquals(0, BigDecimal("0.35").compareTo(step1["user-3"]))

            // Step 2: A is locked at 0.3, type 50% for B → only C adjusts
            val step2 = service.redistributeRemaining(
                editedShare = BigDecimal("0.5"),
                otherMemberIds = listOf("user-1", "user-3"),
                lockedShares = mapOf("user-1" to BigDecimal("0.3"))
            )
            // C = 1 - 0.5 - 0.3 = 0.2
            assertEquals(1, step2.size)
            assertEquals(0, BigDecimal("0.2").compareTo(step2["user-3"]))
        }

        @Test
        fun `all others locked returns empty map`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.3"),
                otherMemberIds = listOf("user-2", "user-3"),
                lockedShares = mapOf(
                    "user-2" to BigDecimal("0.4"),
                    "user-3" to BigDecimal("0.3")
                )
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `locked shares exceeding remaining clamps to 0`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.5"),
                otherMemberIds = listOf("user-2", "user-3"),
                lockedShares = mapOf("user-2" to BigDecimal("0.6"))
            )

            // remaining = max(0, 1 - 0.5 - 0.6) = 0 → user-3 gets 0
            assertEquals(1, result.size)
            assertEquals(0, BigDecimal.ZERO.compareTo(result["user-3"]))
        }

        @Test
        fun `multiple unlocked members share the remainder evenly`() {
            // A edited=40%, B locked=20%, C and D unlocked
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.4"),
                otherMemberIds = listOf("user-2", "user-3", "user-4"),
                lockedShares = mapOf("user-2" to BigDecimal("0.2"))
            )

            // remaining = 1 - 0.4 - 0.2 = 0.4, split between C and D
            assertEquals(2, result.size)
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-3"]))
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-4"]))
        }

        @Test
        fun `30 50 20 sequential edit scenario with 3 members - regression issue 662`() {
            // Simulates the exact ViewModel flow:
            //   otherMemberIds = ALL other selected members (locked + unlocked)
            //   lockedShares = map of locked members' shares

            // Step 1: A types 30% → B and C redistribute evenly (no locks)
            val step1 = service.redistributeRemaining(
                editedShare = BigDecimal("0.3"),
                otherMemberIds = listOf("B", "C")
            )
            assertEquals(0, BigDecimal("0.35").compareTo(step1["B"]))
            assertEquals(0, BigDecimal("0.35").compareTo(step1["C"]))

            // Step 2: B types 50%, A is locked at 30%
            // otherMemberIds includes BOTH A (locked) and C (unlocked)
            val step2 = service.redistributeRemaining(
                editedShare = BigDecimal("0.5"),
                otherMemberIds = listOf("A", "C"),
                lockedShares = mapOf("A" to BigDecimal("0.3"))
            )
            // C should get 1 - 0.5 - 0.3 = 0.2 (NOT 0.5!)
            assertEquals(1, step2.size)
            assertEquals(0, BigDecimal("0.2").compareTo(step2["C"]))

            // Step 3: Verify C is 20% by checking the final shares sum to 1
            val aShare = BigDecimal("0.3")
            val bShare = BigDecimal("0.5")
            val cShare = step2["C"]!!
            val total = aShare.add(bShare).add(cShare)
            assertEquals(0, BigDecimal.ONE.compareTo(total))
        }

        @Test
        fun `empty lockedShares is backward compatible`() {
            val result = service.redistributeRemaining(
                editedShare = BigDecimal("0.6"),
                otherMemberIds = listOf("user-2", "user-3"),
                lockedShares = emptyMap()
            )

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-2"]))
            assertEquals(0, BigDecimal("0.2").compareTo(result["user-3"]))
        }
    }

    @Nested
    @DisplayName("parseShareTexts")
    inner class ParseShareTexts {

        @Test
        fun `parses valid percentage texts`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "60", "user-2" to "40")
            )

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal("0.6").compareTo(result["user-1"]))
            assertEquals(0, BigDecimal("0.4").compareTo(result["user-2"]))
        }

        @Test
        fun `returns empty map when all entries are blank`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "", "user-2" to "")
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty map when share texts map is empty`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1"),
                memberShareTexts = emptyMap()
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty map when any entry is unparseable and non-blank`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "50", "user-2" to "abc")
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `missing member in texts defaults to 0`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "100")
            )

            assertEquals(2, result.size)
            assertEquals(0, BigDecimal.ONE.compareTo(result["user-1"]))
            assertEquals(0, BigDecimal.ZERO.compareTo(result["user-2"]))
        }

        @Test
        fun `parses decimal percentages correctly`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "33.33", "user-2" to "66.67")
            )

            assertEquals(2, result.size)
            assertTrue(result["user-1"]!!.subtract(BigDecimal("0.3333")).abs() < BigDecimal("0.0001"))
            assertTrue(result["user-2"]!!.subtract(BigDecimal("0.6667")).abs() < BigDecimal("0.0001"))
        }

        @Test
        fun `parses comma-decimal percentages correctly (ES locale format)`() {
            val result = service.parseShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "33,33", "user-2" to "66,67")
            )

            assertEquals(2, result.size)
            assertTrue(result["user-1"]!!.subtract(BigDecimal("0.3333")).abs() < BigDecimal("0.0001"))
            assertTrue(result["user-2"]!!.subtract(BigDecimal("0.6667")).abs() < BigDecimal("0.0001"))
        }
    }

    @Nested
    @DisplayName("validateShareTexts")
    inner class ValidateShareTexts {

        @Test
        fun `returns Valid for correct 50-50 shares`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "50", "user-2" to "50")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.Valid)
        }

        @Test
        fun `returns Valid when all shares are blank (auto-normalize)`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "", "user-2" to "")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.Valid)
        }

        @Test
        fun `returns Valid when share texts map is empty`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1"),
                memberShareTexts = emptyMap()
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.Valid)
        }

        @Test
        fun `returns Unparseable when entry is not a number`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "50", "user-2" to "abc")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.Unparseable)
        }

        @Test
        fun `returns OutOfRange when share exceeds 100 percent`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "306", "user-2" to "50")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.OutOfRange)
        }

        @Test
        fun `returns OutOfRange when share is negative`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "-10", "user-2" to "50")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.OutOfRange)
        }

        @Test
        fun `returns SumMismatch when shares total less than 100 percent`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "30", "user-2" to "30")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.SumMismatch)
        }

        @Test
        fun `returns SumMismatch when shares total more than 100 percent`() {
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2"),
                memberShareTexts = mapOf("user-1" to "60", "user-2" to "60")
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.SumMismatch)
        }

        @Test
        fun `returns Valid for shares within tolerance of 100 percent`() {
            // Three-way even split: 33.33 * 3 = 99.99, within 0.1% tolerance
            val result = service.validateShareTexts(
                selectedMemberIds = listOf("user-1", "user-2", "user-3"),
                memberShareTexts = mapOf(
                    "user-1" to "33.33",
                    "user-2" to "33.33",
                    "user-3" to "33.33"
                )
            )

            assertTrue(result is SubunitShareDistributionService.ShareTextValidation.Valid)
        }
    }
}
