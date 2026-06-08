package es.pedrazamiguez.splittrip.domain.service.split

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.EntitySplit
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.SubunitSplitOverride
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SubunitAwareSplitServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubunitAwareSplitService")
class SubunitAwareSplitServiceTest {

    private lateinit var service: SubunitAwareSplitService

    @BeforeEach
    fun setUp() {
        val calculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorServiceImpl())
        service = SubunitAwareSplitServiceImpl(calculatorFactory)
    }

    // ── No subunits (backward compatibility) ───────────────────────────

    @Nested
    @DisplayName("No Subunits (Flat Split)")
    inner class NoSubunits {

        @Test
        fun `equal split with no subunits behaves like standard equal split`() {
            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("user1", "user2"),
                subunits = emptyList(),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(2, result.size)
            assertEquals(5000L, result[0].amountCents)
            assertEquals(5000L, result[1].amountCents)
            assertNull(result[0].subunitId)
            assertNull(result[1].subunitId)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `exact split with no subunits passes through`() {
            val entitySplits = listOf(
                EntitySplit(entityId = "user1", amountCents = 7000L),
                EntitySplit(entityId = "user2", amountCents = 3000L)
            )
            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("user1", "user2"),
                subunits = emptyList(),
                entitySplitType = SplitType.EXACT,
                entitySplits = entitySplits
            )

            assertEquals(2, result.size)
            assertEquals(7000L, result.find { it.userId == "user1" }!!.amountCents)
            assertEquals(3000L, result.find { it.userId == "user2" }!!.amountCents)
        }

        @Test
        fun `percent split with no subunits passes through`() {
            val entitySplits = listOf(
                EntitySplit(entityId = "user1", percentage = BigDecimal("60")),
                EntitySplit(entityId = "user2", percentage = BigDecimal("40"))
            )
            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("user1", "user2"),
                subunits = emptyList(),
                entitySplitType = SplitType.PERCENT,
                entitySplits = entitySplits
            )

            assertEquals(2, result.size)
            assertEquals(6000L, result.find { it.userId == "user1" }!!.amountCents)
            assertEquals(4000L, result.find { it.userId == "user2" }!!.amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }
    }

    // ── EQUAL at entity level, default intra-subunit ───────────────────

    @Nested
    @DisplayName("EQUAL Entity Split — Default Intra-Subunit")
    inner class EqualEntityDefaultIntra {

        @Test
        fun `splits equally among solo user and one couple subunit`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EQUAL
            )

            // 2 entities → 5000 each
            // Couple 5000 split 50/50 → 2500 each
            assertEquals(3, result.size)
            assertEquals(5000L, result.find { it.userId == "solo" }!!.amountCents)
            assertNull(result.find { it.userId == "solo" }!!.subunitId)

            assertEquals(2500L, result.find { it.userId == "userA" }!!.amountCents)
            assertEquals("subunit-couple", result.find { it.userId == "userA" }!!.subunitId)

            assertEquals(2500L, result.find { it.userId == "userB" }!!.amountCents)
            assertEquals("subunit-couple", result.find { it.userId == "userB" }!!.subunitId)

            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `water park example — 4 entities equal split, memberShares proportional`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("antonio", "me"),
                memberShares = mapOf(
                    "antonio" to BigDecimal("0.5"),
                    "me" to BigDecimal("0.5")
                )
            )
            val pair = Subunit(
                id = "subunit-pair",
                memberIds = listOf("miguel", "maria"),
                memberShares = mapOf(
                    "miguel" to BigDecimal("0.5"),
                    "maria" to BigDecimal("0.5")
                )
            )
            val family = Subunit(
                id = "subunit-family",
                memberIds = listOf("ana", "luis", "luisito"),
                memberShares = mapOf(
                    "ana" to BigDecimal("0.4"),
                    "luis" to BigDecimal("0.4"),
                    "luisito" to BigDecimal("0.2")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 20000L,
                individualParticipantIds = listOf("juan"),
                subunits = listOf(couple, pair, family),
                entitySplitType = SplitType.EQUAL
            )

            // 4 entities, 20000 / 4 = 5000 each
            assertEquals(8, result.size)
            assertEquals(5000L, result.find { it.userId == "juan" }!!.amountCents)
            assertNull(result.find { it.userId == "juan" }!!.subunitId)

            // Couple: 5000 * 0.5 = 2500 each
            assertEquals(2500L, result.find { it.userId == "antonio" }!!.amountCents)
            assertEquals(2500L, result.find { it.userId == "me" }!!.amountCents)

            // Pair: 5000 * 0.5 = 2500 each
            assertEquals(2500L, result.find { it.userId == "miguel" }!!.amountCents)
            assertEquals(2500L, result.find { it.userId == "maria" }!!.amountCents)

            // Family: 5000 * 0.4 = 2000, 5000 * 0.4 = 2000, 5000 * 0.2 = 1000
            assertEquals(2000L, result.find { it.userId == "ana" }!!.amountCents)
            assertEquals(2000L, result.find { it.userId == "luis" }!!.amountCents)
            assertEquals(1000L, result.find { it.userId == "luisito" }!!.amountCents)

            assertEquals(20000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `subunit with empty memberShares uses equal split`() {
            val subunit = Subunit(
                id = "subunit-1",
                memberIds = listOf("userA", "userB"),
                memberShares = emptyMap()
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = emptyList(),
                subunits = listOf(subunit),
                entitySplitType = SplitType.EQUAL
            )

            // 1 entity gets 10000, split equally → 5000 each
            assertEquals(2, result.size)
            assertEquals(5000L, result[0].amountCents)
            assertEquals(5000L, result[1].amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `odd amount distributes remainder correctly at entity level`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10001L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(3, result.size)
            // Total must be conserved
            assertEquals(10001L, result.sumOf { it.amountCents })
        }
    }

    // ── EXACT at entity level ───────────────────────────────────────────

    @Nested
    @DisplayName("EXACT Entity Split")
    inner class ExactEntitySplit {

        @Test
        fun `exact entity split with proportional intra-subunit`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val entitySplits = listOf(
                EntitySplit(entityId = "solo", amountCents = 3000L),
                EntitySplit(entityId = "subunit-couple", amountCents = 7000L)
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EXACT,
                entitySplits = entitySplits
            )

            assertEquals(3, result.size)
            assertEquals(3000L, result.find { it.userId == "solo" }!!.amountCents)
            assertEquals(3500L, result.find { it.userId == "userA" }!!.amountCents)
            assertEquals(3500L, result.find { it.userId == "userB" }!!.amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }
    }

    // ── PERCENT at entity level ─────────────────────────────────────────

    @Nested
    @DisplayName("PERCENT Entity Split")
    inner class PercentEntitySplit {

        @Test
        fun `percent entity split distributes correctly`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val entitySplits = listOf(
                EntitySplit(entityId = "solo", percentage = BigDecimal("30")),
                EntitySplit(entityId = "subunit-couple", percentage = BigDecimal("70"))
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.PERCENT,
                entitySplits = entitySplits
            )

            assertEquals(3, result.size)
            assertEquals(3000L, result.find { it.userId == "solo" }!!.amountCents)
            // Couple gets 7000, split 50/50
            assertEquals(3500L, result.find { it.userId == "userA" }!!.amountCents)
            assertEquals(3500L, result.find { it.userId == "userB" }!!.amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `percent entity split computes effective percentages for subunit members`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val entitySplits = listOf(
                EntitySplit(entityId = "solo", percentage = BigDecimal("30")),
                EntitySplit(entityId = "subunit-couple", percentage = BigDecimal("70"))
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.PERCENT,
                entitySplits = entitySplits
            )

            // Solo user keeps their entity-level percentage
            val solo = result.find { it.userId == "solo" }!!
            assertNotNull(solo.percentage)
            assertEquals(BigDecimal("30"), solo.percentage)

            // Subunit members get computed effective percentages (35% each of total)
            val userA = result.find { it.userId == "userA" }!!
            val userB = result.find { it.userId == "userB" }!!
            assertNotNull(userA.percentage)
            assertNotNull(userB.percentage)
            assertEquals(0, BigDecimal("35.00").compareTo(userA.percentage))
            assertEquals(0, BigDecimal("35.00").compareTo(userB.percentage))
        }
    }

    // ── Intra-subunit overrides ────────────────────────────────────────

    @Nested
    @DisplayName("Intra-Subunit Overrides")
    inner class IntraSubunitOverrides {

        @Test
        fun `EQUAL entity split with EXACT override for one subunit`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val overrides = mapOf(
                "subunit-couple" to SubunitSplitOverride(
                    splitType = SplitType.EXACT,
                    memberSplits = listOf(
                        ExpenseSplit(userId = "userA", amountCents = 4000L),
                        ExpenseSplit(userId = "userB", amountCents = 1000L)
                    )
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EQUAL,
                subunitSplitOverrides = overrides
            )

            assertEquals(3, result.size)
            assertEquals(5000L, result.find { it.userId == "solo" }!!.amountCents)
            // Couple gets 5000, but override says A=4000, B=1000
            assertEquals(4000L, result.find { it.userId == "userA" }!!.amountCents)
            assertEquals(1000L, result.find { it.userId == "userB" }!!.amountCents)
            assertEquals("subunit-couple", result.find { it.userId == "userA" }!!.subunitId)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `EQUAL entity split with EQUAL override for subunit`() {
            val family = Subunit(
                id = "subunit-family",
                memberIds = listOf("parent1", "parent2", "child"),
                memberShares = mapOf(
                    "parent1" to BigDecimal("0.4"),
                    "parent2" to BigDecimal("0.3"),
                    "child" to BigDecimal("0.3")
                )
            )

            val overrides = mapOf(
                "subunit-family" to SubunitSplitOverride(splitType = SplitType.EQUAL)
            )

            val result = service.calculateShares(
                totalAmountCents = 9000L,
                individualParticipantIds = emptyList(),
                subunits = listOf(family),
                entitySplitType = SplitType.EQUAL,
                subunitSplitOverrides = overrides
            )

            // 1 entity gets 9000, EQUAL among 3 members → 3000 each
            assertEquals(3, result.size)
            assertEquals(3000L, result.find { it.userId == "parent1" }!!.amountCents)
            assertEquals(3000L, result.find { it.userId == "parent2" }!!.amountCents)
            assertEquals(3000L, result.find { it.userId == "child" }!!.amountCents)
            assertEquals(9000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `EQUAL entity split with PERCENT override for subunit`() {
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val overrides = mapOf(
                "subunit-couple" to SubunitSplitOverride(
                    splitType = SplitType.PERCENT,
                    memberSplits = listOf(
                        ExpenseSplit(userId = "userA", amountCents = 0, percentage = BigDecimal("70")),
                        ExpenseSplit(userId = "userB", amountCents = 0, percentage = BigDecimal("30"))
                    )
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("solo"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EQUAL,
                subunitSplitOverrides = overrides
            )

            assertEquals(3, result.size)
            assertEquals(5000L, result.find { it.userId == "solo" }!!.amountCents)
            // Couple gets 5000, override 70/30 → 3500 + 1500
            assertEquals(3500L, result.find { it.userId == "userA" }!!.amountCents)
            assertEquals(1500L, result.find { it.userId == "userB" }!!.amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `mixed overrides — different split types per subunit`() {
            // Use 9000 to divide evenly: 9000 / 3 entities = 3000 each
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )
            val family = Subunit(
                id = "subunit-family",
                memberIds = listOf("ana", "luis", "luisito"),
                memberShares = mapOf(
                    "ana" to BigDecimal("0.4"),
                    "luis" to BigDecimal("0.4"),
                    "luisito" to BigDecimal("0.2")
                )
            )

            // Family gets 3000 from entity-level EQUAL split
            val overrides = mapOf(
                "subunit-couple" to SubunitSplitOverride(splitType = SplitType.EQUAL),
                "subunit-family" to SubunitSplitOverride(
                    splitType = SplitType.EXACT,
                    memberSplits = listOf(
                        ExpenseSplit(userId = "ana", amountCents = 1500L),
                        ExpenseSplit(userId = "luis", amountCents = 1500L),
                        ExpenseSplit(userId = "luisito", amountCents = 0L)
                    )
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 9000L,
                individualParticipantIds = listOf("juan"),
                subunits = listOf(couple, family),
                entitySplitType = SplitType.EQUAL,
                subunitSplitOverrides = overrides
            )

            // 3 entities (juan, couple, family) → 3000 each
            assertEquals(6, result.size)
            assertEquals(9000L, result.sumOf { it.amountCents })

            // Juan is solo → 3000
            assertEquals(3000L, result.find { it.userId == "juan" }!!.amountCents)
            assertNull(result.find { it.userId == "juan" }!!.subunitId)

            // Couple: EQUAL override → 1500 each
            val coupleMembers = result.filter { it.subunitId == "subunit-couple" }
            assertEquals(2, coupleMembers.size)
            assertEquals(1500L, coupleMembers[0].amountCents)
            assertEquals(1500L, coupleMembers[1].amountCents)

            // Family: EXACT override → luisito gets 0
            assertEquals(1500L, result.find { it.userId == "ana" }!!.amountCents)
            assertEquals(1500L, result.find { it.userId == "luis" }!!.amountCents)
            assertEquals(0L, result.find { it.userId == "luisito" }!!.amountCents)
        }
    }

    // ── Edge cases ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `single member subunit`() {
            val solo = Subunit(
                id = "subunit-solo",
                memberIds = listOf("userA"),
                memberShares = mapOf("userA" to BigDecimal("1.0"))
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = emptyList(),
                subunits = listOf(solo),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(1, result.size)
            assertEquals(10000L, result[0].amountCents)
            assertEquals("subunit-solo", result[0].subunitId)
        }

        @Test
        fun `all solo users with no subunits`() {
            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = listOf("user1", "user2", "user3"),
                subunits = emptyList(),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(3, result.size)
            result.forEach { assertNull(it.subunitId) }
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `remainder distribution preserves total at both levels`() {
            // 3 entities, 10001 cents — remainder at entity level
            // One couple with 50/50 — even intra-subunit
            val couple = Subunit(
                id = "subunit-couple",
                memberIds = listOf("userA", "userB"),
                memberShares = mapOf(
                    "userA" to BigDecimal("0.5"),
                    "userB" to BigDecimal("0.5")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10001L,
                individualParticipantIds = listOf("solo1", "solo2"),
                subunits = listOf(couple),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(4, result.size)
            assertEquals(10001L, result.sumOf { it.amountCents })
        }

        @Test
        fun `subunits with uneven memberShares distribute correctly`() {
            val family = Subunit(
                id = "subunit-family",
                memberIds = listOf("parent", "child"),
                memberShares = mapOf(
                    "parent" to BigDecimal("0.7"),
                    "child" to BigDecimal("0.3")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = emptyList(),
                subunits = listOf(family),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(2, result.size)
            assertEquals(7000L, result.find { it.userId == "parent" }!!.amountCents)
            assertEquals(3000L, result.find { it.userId == "child" }!!.amountCents)
            assertEquals(10000L, result.sumOf { it.amountCents })
        }

        @Test
        fun `three members with one-third shares conserve total`() {
            val trio = Subunit(
                id = "subunit-trio",
                memberIds = listOf("a", "b", "c"),
                memberShares = mapOf(
                    "a" to BigDecimal("0.3333333333"),
                    "b" to BigDecimal("0.3333333333"),
                    "c" to BigDecimal("0.3333333334")
                )
            )

            val result = service.calculateShares(
                totalAmountCents = 10000L,
                individualParticipantIds = emptyList(),
                subunits = listOf(trio),
                entitySplitType = SplitType.EQUAL
            )

            assertEquals(3, result.size)
            // Total must be conserved even with repeating decimal shares
            assertEquals(10000L, result.sumOf { it.amountCents })
        }
    }

    // ── distributeByMemberShares — deterministic ordering ─────────────────

    @Nested
    @DisplayName("distributeByMemberShares — Deterministic Ordering")
    inner class DistributeByMemberSharesOrdering {

        @Test
        fun `remainder allocation is deterministic regardless of memberIds input order`() {
            val shares = mapOf(
                "charlie" to BigDecimal("0.3333333333"),
                "alice" to BigDecimal("0.3333333333"),
                "bob" to BigDecimal("0.3333333334")
            )

            val resultAsc = service.distributeByMemberShares(
                listOf("alice", "bob", "charlie"),
                10000L,
                shares
            )
            val resultDesc = service.distributeByMemberShares(
                listOf("charlie", "bob", "alice"),
                10000L,
                shares
            )

            assertEquals(resultAsc, resultDesc)
            assertEquals(10000L, resultAsc.values.sum())
        }
    }
}
