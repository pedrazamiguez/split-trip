package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SubunitAwareSplitServiceImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("IntraSubunitSplitDelegate")
class IntraSubunitSplitDelegateTest {

    private lateinit var delegate: IntraSubunitSplitDelegate
    private lateinit var formattingHelper: FormattingHelper

    private val equalType = SplitTypeUiModel(id = "EQUAL", displayText = "Equal")
    private val exactType = SplitTypeUiModel(id = "EXACT", displayText = "Exact")
    private val percentType = SplitTypeUiModel(id = "PERCENT", displayText = "Percent")

    private val member1 = "member-1"
    private val member2 = "member-2"
    private val member3 = "member-3"
    private val subunitId = "subunit-1"

    private val coupleSubunit = Subunit(
        id = subunitId,
        groupId = "group-1",
        name = "The Couple",
        memberIds = listOf(member1, member2),
        memberShares = mapOf(
            member1 to BigDecimal("0.6"),
            member2 to BigDecimal("0.4")
        )
    )

    private val evenSubunit = Subunit(
        id = subunitId,
        groupId = "group-1",
        name = "Even Subunit",
        memberIds = listOf(member1, member2),
        memberShares = emptyMap()
    )

    private fun makeMember(
        userId: String,
        amountCents: Long = 0L,
        amountInput: String = "",
        percentageInput: String = ""
    ) = SplitUiModel(
        userId = userId,
        displayName = userId,
        amountCents = amountCents,
        amountInput = amountInput,
        percentageInput = percentageInput
    )

    private fun makeEntity(
        entityId: String = subunitId,
        amountCents: Long = 10000L,
        members: List<SplitUiModel> = listOf(makeMember(member1), makeMember(member2)),
        splitType: SplitTypeUiModel? = equalType,
        isExcluded: Boolean = false
    ) = SplitUiModel(
        userId = entityId,
        displayName = "Entity",
        amountCents = amountCents,
        isEntityRow = true,
        entityMembers = members.toImmutableList(),
        entitySplitType = splitType,
        isExcluded = isExcluded
    )

    @BeforeEach
    fun setUp() {
        val localeProvider = mockk<LocaleProvider>()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        formattingHelper = FormattingHelper(localeProvider)
        val splitCalculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorServiceImpl())
        val splitPreviewService = SplitPreviewServiceImpl()
        val subunitAwareSplitService = SubunitAwareSplitServiceImpl(splitCalculatorFactory)

        delegate = IntraSubunitSplitDelegate(
            splitCalculatorFactory = splitCalculatorFactory,
            splitPreviewService = splitPreviewService,
            subunitAwareSplitService = subunitAwareSplitService,
            formattingHelper = formattingHelper
        )
    }

    // ── recalculate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("recalculate")
    inner class Recalculate {

        @Test
        fun `returns entity unchanged when entityMembers is empty`() {
            val entity = makeEntity(members = emptyList())
            val result = delegate.recalculate(entity, "EUR", listOf(coupleSubunit), 2)
            assertEquals(entity, result)
        }

        @Test
        fun `returns entity unchanged when subunitTotalCents is zero`() {
            val entity = makeEntity(amountCents = 0)
            val result = delegate.recalculate(entity, "EUR", listOf(coupleSubunit), 2)
            assertEquals(entity, result)
        }

        @Test
        fun `returns entity unchanged when subunitTotalCents is negative`() {
            val entity = makeEntity(amountCents = -100)
            val result = delegate.recalculate(entity, "EUR", listOf(coupleSubunit), 2)
            assertEquals(entity, result)
        }

        @Test
        fun `EQUAL split with memberShares distributes by weight`() {
            // 100 EUR, 60/40 split
            val entity = makeEntity(amountCents = 10000L, splitType = equalType)
            val result = delegate.recalculate(entity, "EUR", listOf(coupleSubunit), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            assertEquals(6000L, members[0].amountCents)
            assertEquals(4000L, members[1].amountCents)
            assertTrue(members[0].formattedAmount.isNotBlank())
            assertTrue(members[1].formattedAmount.isNotBlank())
        }

        @Test
        fun `EQUAL split without memberShares falls back to even split`() {
            val entity = makeEntity(amountCents = 10000L, splitType = equalType)
            val result = delegate.recalculate(entity, "EUR", listOf(evenSubunit), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            assertEquals(5000L, members[0].amountCents)
            assertEquals(5000L, members[1].amountCents)
        }

        @Test
        fun `EQUAL split with no matching subunit falls back to even split`() {
            val entity = makeEntity(amountCents = 10000L, splitType = equalType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            assertEquals(5000L, members[0].amountCents)
            assertEquals(5000L, members[1].amountCents)
        }

        @Test
        fun `EQUAL split with odd amount distributes remainder`() {
            // 99 cents across 2 members = 50 + 49 (or 49 + 50)
            val entity = makeEntity(amountCents = 99L, splitType = equalType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            val total = members.sumOf { it.amountCents }
            assertEquals(99L, total)
        }

        @Test
        fun `EXACT split pre-fills with even distribution`() {
            val entity = makeEntity(amountCents = 10000L, splitType = exactType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            assertEquals(5000L, members[0].amountCents)
            assertEquals(5000L, members[1].amountCents)
            // EXACT mode should populate amountInput
            assertTrue(members[0].amountInput.isNotBlank())
            assertTrue(members[1].amountInput.isNotBlank())
        }

        @Test
        fun `EXACT split with 3 members distributes evenly`() {
            val threeMembers = listOf(makeMember(member1), makeMember(member2), makeMember(member3))
            val entity = makeEntity(amountCents = 10000L, splitType = exactType, members = threeMembers)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(3, members.size)
            val total = members.sumOf { it.amountCents }
            assertEquals(10000L, total)
        }

        @Test
        fun `PERCENT split pre-fills with even percentages`() {
            val entity = makeEntity(amountCents = 10000L, splitType = percentType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            // Each member should get 50%
            assertTrue(members[0].percentageInput.isNotBlank())
            assertTrue(members[1].percentageInput.isNotBlank())
            assertEquals(5000L, members[0].amountCents)
            assertEquals(5000L, members[1].amountCents)
        }

        @Test
        fun `PERCENT split with 3 members distributes percentages`() {
            val threeMembers = listOf(makeMember(member1), makeMember(member2), makeMember(member3))
            val entity = makeEntity(amountCents = 10000L, splitType = percentType, members = threeMembers)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(3, members.size)
            val total = members.sumOf { it.amountCents }
            assertEquals(10000L, total)
            members.forEach { assertTrue(it.percentageInput.isNotBlank()) }
        }

        @Test
        fun `PERCENT split shows formatted amount for positive total`() {
            val entity = makeEntity(amountCents = 10000L, splitType = percentType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            result.entityMembers.forEach {
                assertTrue(it.formattedAmount.isNotBlank())
            }
        }

        @Test
        fun `defaults to EQUAL when entitySplitType is null`() {
            val entity = makeEntity(amountCents = 10000L, splitType = null)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(2, members.size)
            // Should behave as EQUAL
            assertEquals(5000L, members[0].amountCents)
            assertEquals(5000L, members[1].amountCents)
        }

        @Test
        fun `EQUAL split with memberShares and remainder is distributed correctly`() {
            // 101 cents, 60/40 → 60.6 + 40.4 → floor: 60 + 40 = 100, remainder 1 → 61 + 40
            val entity = makeEntity(amountCents = 101L, splitType = equalType)
            val result = delegate.recalculate(entity, "EUR", listOf(coupleSubunit), 2)

            val total = result.entityMembers.sumOf { it.amountCents }
            assertEquals(101L, total)
        }
    }

    // ── parseSourceAmountToCents ─────────────────────────────────────────

    @Nested
    @DisplayName("parseSourceAmountToCents")
    inner class ParseSourceAmountToCents {

        @Test
        fun `parses standard amount with 2 decimal places`() {
            assertEquals(10050L, delegate.parseSourceAmountToCents("100.50", 2))
        }

        @Test
        fun `parses zero decimal currency`() {
            assertEquals(100L, delegate.parseSourceAmountToCents("100", 0))
        }

        @Test
        fun `trims whitespace before parsing`() {
            assertEquals(10050L, delegate.parseSourceAmountToCents("  100.50  ", 2))
        }

        @Test
        fun `returns zero for empty string`() {
            assertEquals(0L, delegate.parseSourceAmountToCents("", 2))
        }
    }

    // ── distributeEntitySplits ───────────────────────────────────────────

    @Nested
    @DisplayName("distributeEntitySplits")
    inner class DistributeEntitySplits {

        private val solo1Entity = makeEntity(entityId = "solo-1", members = emptyList())
        private val solo2Entity = makeEntity(entityId = "solo-2", members = emptyList())
        private val subunitEntity = makeEntity(
            entityId = subunitId,
            members = listOf(makeMember(member1), makeMember(member2))
        )

        private val entitySplits = persistentListOf(solo1Entity, solo2Entity, subunitEntity)
        private val activeIds = listOf("solo-1", "solo-2", subunitId)

        @Test
        fun `returns null when activeEntityIds is empty`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 30000L,
                activeEntityIds = emptyList(),
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit),
                decimalDigits = 2
            )
            assertNull(result)
        }

        @Test
        fun `EQUAL distribution returns null when sourceAmountCents is zero`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 0L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit),
                decimalDigits = 2
            )
            assertNull(result)
        }

        @Test
        fun `EQUAL distribution splits evenly among entities`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 30000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit),
                decimalDigits = 2
            )

            assertNotNull(result)
            assertEquals(3, result!!.size)
            assertEquals(10000L, result[0].amountCents)
            assertEquals(10000L, result[1].amountCents)
            assertEquals(10000L, result[2].amountCents)
        }

        @Test
        fun `EQUAL distribution also recalculates intra-subunit members`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 30000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit),
                decimalDigits = 2
            )

            assertNotNull(result)
            // The subunit entity (index 2) should have its members recalculated
            val subunit = result!![2]
            assertEquals(2, subunit.entityMembers.size)
            // With coupleSubunit shares (60/40): 10000 * 0.6 = 6000, 10000 * 0.4 = 4000
            assertEquals(6000L, subunit.entityMembers[0].amountCents)
            assertEquals(4000L, subunit.entityMembers[1].amountCents)
        }

        @Test
        fun `EQUAL distribution zeroes excluded entities`() {
            val excludedEntity = solo1Entity.copy(isExcluded = true)
            val splits = persistentListOf(excludedEntity, solo2Entity, subunitEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 30000L,
                activeEntityIds = listOf("solo-2", subunitId),
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit),
                decimalDigits = 2
            )

            assertNotNull(result)
            assertEquals(0L, result!![0].amountCents)
            assertEquals("", result[0].formattedAmount)
            // Remaining 30000 split between 2 entities: 15000 each
            assertEquals(15000L, result[1].amountCents)
            assertEquals(15000L, result[2].amountCents)
        }

        @Test
        fun `EXACT distribution returns null when sourceAmountCents is zero`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EXACT,
                sourceAmountCents = 0L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )
            assertNull(result)
        }

        @Test
        fun `EXACT distribution pre-fills with even amounts and inputs`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EXACT,
                sourceAmountCents = 30000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            result!!.filter { !it.isExcluded }.forEach { entity ->
                assertEquals(10000L, entity.amountCents)
                assertTrue(entity.amountInput.isNotBlank())
                assertTrue(entity.formattedAmount.isNotBlank())
            }
        }

        @Test
        fun `EXACT distribution zeroes excluded entities`() {
            val excludedEntity = solo1Entity.copy(isExcluded = true)
            val splits = persistentListOf(excludedEntity, solo2Entity, subunitEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.EXACT,
                sourceAmountCents = 20000L,
                activeEntityIds = listOf("solo-2", subunitId),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            assertEquals(0L, result!![0].amountCents)
            assertEquals("", result[0].amountInput)
        }

        @Test
        fun `PERCENT distribution fills percentages and amounts`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.PERCENT,
                sourceAmountCents = 30000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            result!!.filter { !it.isExcluded }.forEach { entity ->
                assertTrue(entity.percentageInput.isNotBlank())
                assertTrue(entity.amountCents > 0)
                assertTrue(entity.formattedAmount.isNotBlank())
            }
            // Total should equal source amount
            val total = result.sumOf { it.amountCents }
            assertEquals(30000L, total)
        }

        @Test
        fun `PERCENT distribution zeroes excluded entities`() {
            val excludedEntity = solo1Entity.copy(isExcluded = true)
            val splits = persistentListOf(excludedEntity, solo2Entity, subunitEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.PERCENT,
                sourceAmountCents = 20000L,
                activeEntityIds = listOf("solo-2", subunitId),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            assertEquals(0L, result!![0].amountCents)
            assertEquals("", result[0].percentageInput)
        }

        @Test
        fun `PERCENT distribution with zero source amount shows empty formatted amounts`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.PERCENT,
                sourceAmountCents = 0L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            result!!.filter { !it.isExcluded }.forEach { entity ->
                // Percentages should still be filled, but amounts should be 0
                assertTrue(entity.percentageInput.isNotBlank())
                assertEquals(0L, entity.amountCents)
            }
        }

        @Test
        fun `EQUAL distribution with negative sourceAmountCents returns null`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = -1000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )
            assertNull(result)
        }

        @Test
        fun `EXACT distribution with negative sourceAmountCents returns null`() {
            val result = delegate.distributeEntitySplits(
                entitySplits = entitySplits,
                splitType = SplitType.EXACT,
                sourceAmountCents = -1000L,
                activeEntityIds = activeIds,
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )
            assertNull(result)
        }
    }

    // ── Internal method coverage ─────────────────────────────────────────

    @Nested
    @DisplayName("recalculateEqual — branch coverage")
    inner class RecalculateEqualBranches {

        @Test
        fun `member not found in shares map returns unchanged`() {
            // member3 is not in the subunit, so its share won't be found
            val threeMembers = listOf(
                makeMember(member1),
                makeMember(member2),
                makeMember(member3)
            )
            val entity = makeEntity(amountCents = 10000L, splitType = equalType, members = threeMembers)
            // Even subunit doesn't have member3 listed in memberIds but we're
            // testing the calculator fallback path where share lookup misses
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(3, members.size)
            // All three should be calculated by the equal calculator
            val total = members.sumOf { it.amountCents }
            assertEquals(10000L, total)
        }
    }

    @Nested
    @DisplayName("recalculateExact — branch coverage")
    inner class RecalculateExactBranches {

        @Test
        fun `exact split formats amountInput with correct decimal digits`() {
            val entity = makeEntity(amountCents = 10000L, splitType = exactType)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            // Should contain decimal-formatted strings like "50.00"
            assertTrue(members[0].amountInput.contains("."))
        }

        @Test
        fun `exact split with zero-decimal currency`() {
            val entity = makeEntity(amountCents = 100L, splitType = exactType)
            val result = delegate.recalculate(entity, "JPY", emptyList(), 0)

            val members = result.entityMembers
            assertEquals(2, members.size)
            val total = members.sumOf { it.amountCents }
            assertEquals(100L, total)
        }
    }

    @Nested
    @DisplayName("recalculatePercent — branch coverage")
    inner class RecalculatePercentBranches {

        @Test
        fun `percent split with single member gives 100 percent`() {
            val singleMember = listOf(makeMember(member1))
            val entity = makeEntity(amountCents = 10000L, splitType = percentType, members = singleMember)
            val result = delegate.recalculate(entity, "EUR", emptyList(), 2)

            val members = result.entityMembers
            assertEquals(1, members.size)
            assertEquals(10000L, members[0].amountCents)
            assertTrue(members[0].percentageInput.contains("100"))
        }
    }

    // ── else-member branches (share not found for a member) ──────────────

    @Nested
    @DisplayName("recalculateEqual — else-member branch (share not found)")
    inner class RecalculateEqualElseMember {

        @Test
        fun `member not in memberIds list is returned unchanged`() {
            // Construct entityMembers with three members but only pass two in memberIds.
            // member3 is in entityMembers but NOT in memberIds → share will be null → else branch.
            val threeMembers = listOf(
                makeMember(member1),
                makeMember(member2),
                makeMember(member3, amountCents = 999L)
            )
            val entity = makeEntity(amountCents = 10000L, splitType = equalType, members = threeMembers)

            val result = delegate.recalculateEqual(
                entity = entity,
                subunitTotalCents = 10000L,
                memberIds = listOf(member1, member2), // member3 deliberately excluded
                currencyCode = "EUR",
                groupSubunits = emptyList()
            )

            val member3Result = result.find { it.userId == member3 }
            assertNotNull(member3Result)
            // member3 should be unchanged (amountCents stays at seed value)
            assertEquals(999L, member3Result!!.amountCents)
        }
    }

    @Nested
    @DisplayName("recalculateExact — else-member branch (share not found)")
    inner class RecalculateExactElseMember {

        @Test
        fun `member not in memberIds list is returned unchanged`() {
            val threeMembers = listOf(
                makeMember(member1),
                makeMember(member2),
                makeMember(member3, amountCents = 777L, amountInput = "7.77")
            )
            val entity = makeEntity(amountCents = 10000L, splitType = exactType, members = threeMembers)

            val result = delegate.recalculateExact(
                entity = entity,
                subunitTotalCents = 10000L,
                memberIds = listOf(member1, member2), // member3 excluded from calculation
                currencyCode = "EUR",
                decimalDigits = 2
            )

            val member3Result = result.find { it.userId == member3 }
            assertNotNull(member3Result)
            assertEquals(777L, member3Result!!.amountCents)
            assertEquals("7.77", member3Result.amountInput)
        }
    }

    // ── else-entity branches (entity not excluded, not in activeIds) ─────

    @Nested
    @DisplayName("distributeEntitySplits — else-entity branches")
    inner class DistributeEntitySplitsElseEntity {

        private val inactiveEntity = makeEntity(
            entityId = "inactive",
            amountCents = 999L,
            members = emptyList(),
            isExcluded = false
        )
        private val activeEntity = makeEntity(
            entityId = "active-1",
            amountCents = 0L,
            members = emptyList()
        )

        @Test
        fun `EQUAL — entity not in activeIds and not excluded is returned unchanged`() {
            val splits = persistentListOf(inactiveEntity, activeEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.EQUAL,
                sourceAmountCents = 10000L,
                activeEntityIds = listOf("active-1"), // "inactive" is NOT here
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            // The inactive entity must be returned as-is (else { entity })
            assertEquals(999L, result!![0].amountCents)
            // The active entity gets the full amount
            assertEquals(10000L, result[1].amountCents)
        }

        @Test
        fun `EXACT — entity not in activeIds and not excluded is returned unchanged`() {
            val splits = persistentListOf(inactiveEntity, activeEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.EXACT,
                sourceAmountCents = 10000L,
                activeEntityIds = listOf("active-1"),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            assertEquals(999L, result!![0].amountCents)
            assertEquals(10000L, result[1].amountCents)
        }

        @Test
        fun `PERCENT — entity not in activeIds and not excluded is returned unchanged`() {
            val splits = persistentListOf(inactiveEntity, activeEntity)

            val result = delegate.distributeEntitySplits(
                entitySplits = splits,
                splitType = SplitType.PERCENT,
                sourceAmountCents = 10000L,
                activeEntityIds = listOf("active-1"),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNotNull(result)
            // inactive entity not in activeIds → else { entity } → amountCents stays at 999
            assertEquals(999L, result!![0].amountCents)
        }
    }

    // ── Exception paths (mocked factory) ────────────────────────────────

    @Nested
    @DisplayName("exception paths — mocked factory throws")
    inner class ExceptionPaths {

        private lateinit var mockedDelegate: IntraSubunitSplitDelegate

        @BeforeEach
        fun setUpWithThrowingFactory() {
            val throwingFactory = mockk<ExpenseSplitCalculatorFactory> {
                every { create(any()) } throws RuntimeException("Simulated calculator failure")
            }
            mockedDelegate = IntraSubunitSplitDelegate(
                splitCalculatorFactory = throwingFactory,
                splitPreviewService = SplitPreviewServiceImpl(),
                subunitAwareSplitService = SubunitAwareSplitServiceImpl(throwingFactory),
                formattingHelper = formattingHelper
            )
        }

        @Test
        fun `recalculateEqual catch block returns entityMembers unchanged on exception`() {
            // No memberShares → goes to calculator path → factory throws → catch fires
            val entity = makeEntity(amountCents = 10000L, splitType = equalType)

            val result = mockedDelegate.recalculateEqual(
                entity = entity,
                subunitTotalCents = 10000L,
                memberIds = listOf(member1, member2),
                currencyCode = "EUR",
                groupSubunits = emptyList() // empty → no memberShares → uses factory
            )

            assertEquals(entity.entityMembers, result)
        }

        @Test
        fun `recalculateExact catch block returns entityMembers unchanged on exception`() {
            val entity = makeEntity(amountCents = 10000L, splitType = exactType)

            val result = mockedDelegate.recalculateExact(
                entity = entity,
                subunitTotalCents = 10000L,
                memberIds = listOf(member1, member2),
                currencyCode = "EUR",
                decimalDigits = 2
            )

            assertEquals(entity.entityMembers, result)
        }

        @Test
        fun `distributeEqualEntities catch block returns null on exception`() {
            val entity = makeEntity(entityId = "e1", members = listOf(makeMember(member1)))

            val result = mockedDelegate.distributeEntitySplits(
                entitySplits = persistentListOf(entity),
                splitType = SplitType.EQUAL,
                sourceAmountCents = 10000L,
                activeEntityIds = listOf("e1"),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNull(result)
        }

        @Test
        fun `distributeExactEntities catch block returns null on exception`() {
            val entity = makeEntity(entityId = "e1", members = listOf(makeMember(member1)))

            val result = mockedDelegate.distributeEntitySplits(
                entitySplits = persistentListOf(entity),
                splitType = SplitType.EXACT,
                sourceAmountCents = 10000L,
                activeEntityIds = listOf("e1"),
                currencyCode = "EUR",
                groupSubunits = emptyList(),
                decimalDigits = 2
            )

            assertNull(result)
        }

        @Test
        fun `recalculate EQUAL path gracefully returns entity on exception`() {
            val entity = makeEntity(amountCents = 10000L, splitType = equalType)

            // recalculate calls recalculateEqual internally; catch block returns entity.entityMembers
            val result = mockedDelegate.recalculate(entity, "EUR", emptyList(), 2)

            assertEquals(entity.entityMembers, result.entityMembers)
        }

        @Test
        fun `recalculate EXACT path gracefully returns entity on exception`() {
            val entity = makeEntity(amountCents = 10000L, splitType = exactType)

            val result = mockedDelegate.recalculate(entity, "EUR", emptyList(), 2)

            assertEquals(entity.entityMembers, result.entityMembers)
        }
    }

    // ── distributeByMemberShares — member not in distributed map ────────

    @Nested
    @DisplayName("recalculateEqual — memberShares path edge cases")
    inner class RecalculateEqualMemberSharesEdgeCases {

        @Test
        fun `member not in memberShares gets zero amount`() {
            // coupleSubunit has only member1 and member2 with 60/40 shares.
            // Add member3 to entity.entityMembers who is NOT in memberShares.
            val threeMembers = listOf(
                makeMember(member1),
                makeMember(member2),
                makeMember(member3, amountCents = 500L)
            )
            val entity = makeEntity(
                amountCents = 10000L,
                splitType = equalType,
                members = threeMembers
            )
            // coupleSubunit only has memberShares for member1 (0.6) and member2 (0.4)
            val result = delegate.recalculateEqual(
                entity = entity,
                subunitTotalCents = 10000L,
                memberIds = listOf(member1, member2, member3),
                currencyCode = "EUR",
                groupSubunits = listOf(coupleSubunit)
            )

            val member3Result = result.find { it.userId == member3 }
            assertNotNull(member3Result)
            // member3 has no share weight → gets 0 from distributeByMemberShares fallback
            assertEquals(0L, member3Result!!.amountCents)
        }

        @Test
        fun `EQUAL with memberShares false condition — empty shares falls through to equal`() {
            // evenSubunit has memberShares = emptyMap() → isEmpty() → uses calculator path
            val entity = makeEntity(amountCents = 6000L, splitType = equalType)
            val result = delegate.recalculateEqual(
                entity = entity,
                subunitTotalCents = 6000L,
                memberIds = listOf(member1, member2),
                currencyCode = "EUR",
                groupSubunits = listOf(evenSubunit)
            )

            val total = result.sumOf { it.amountCents }
            assertEquals(6000L, total)
            assertFalse(result.any { it.formattedAmount.isBlank() })
        }
    }
}
