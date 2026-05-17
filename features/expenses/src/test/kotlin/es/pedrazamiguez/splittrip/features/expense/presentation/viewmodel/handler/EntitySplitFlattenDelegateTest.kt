package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [EntitySplitFlattenDelegate].
 *
 * Uses real [SplitPreviewService] and [RemainderDistributionService].
 */
class EntitySplitFlattenDelegateTest {

    private lateinit var delegate: EntitySplitFlattenDelegate

    private fun makeSplit(
        userId: String,
        amountCents: Long = 0L,
        percentageInput: String = "",
        isExcluded: Boolean = false,
        isEntityRow: Boolean = false,
        entityMembers: List<SplitUiModel> = emptyList(),
        subunitId: String? = null
    ) = SplitUiModel(
        userId = userId,
        displayName = userId,
        amountCents = amountCents,
        percentageInput = percentageInput,
        isExcluded = isExcluded,
        isEntityRow = isEntityRow,
        entityMembers = persistentListOf(*entityMembers.toTypedArray()),
        subunitId = subunitId
    )

    @BeforeEach
    fun setUp() {
        delegate = EntitySplitFlattenDelegate(
            splitPreviewService = SplitPreviewService(),
            remainderDistributionService = RemainderDistributionService()
        )
    }

    // ── flattenEntities ──────────────────────────────────────────────────

    @Nested
    inner class FlattenEntities {

        @Test
        fun `solo entity produces a single split`() {
            val entities = listOf(makeSplit("u1", amountCents = 5000L, isEntityRow = true))

            val result = delegate.flattenEntities(entities, SplitType.EQUAL)

            assertEquals(1, result.size)
            assertEquals("u1", result[0].userId)
            assertEquals(5000L, result[0].amountCents)
        }

        @Test
        fun `subunit entity flattens to member splits`() {
            val members = listOf(
                makeSplit("m1", amountCents = 3000L, subunitId = "sub1"),
                makeSplit("m2", amountCents = 2000L, subunitId = "sub1")
            )
            val entities = listOf(
                makeSplit("sub1", isEntityRow = true, entityMembers = members)
            )

            val result = delegate.flattenEntities(entities, SplitType.EQUAL)

            assertEquals(2, result.size)
            assertEquals("m1", result[0].userId)
            assertEquals(3000L, result[0].amountCents)
            assertEquals("sub1", result[0].subunitId)
            assertEquals("m2", result[1].userId)
        }

        @Test
        fun `subunit entity flattenEntities propagates EQUAL intra-type to member splitType`() {
            val members = listOf(makeSplit("m1", amountCents = 3000L, subunitId = "sub1"))
            val entities = listOf(makeSplit("sub1", isEntityRow = true, entityMembers = members))

            val result = delegate.flattenEntities(entities, SplitType.EQUAL)

            assertEquals(SplitType.EQUAL, result[0].splitType)
        }

        @Test
        fun `solo entity splitType is null regardless of outer splitType`() {
            val entities = listOf(
                makeSplit("u1", amountCents = 5000L, isEntityRow = true)
            )

            val result = delegate.flattenEntities(entities, SplitType.PERCENT)

            assertNull(result[0].splitType)
        }

        @Test
        fun `excluded entities are skipped`() {
            val entities = listOf(
                makeSplit("u1", amountCents = 5000L, isEntityRow = true),
                makeSplit("u2", amountCents = 5000L, isEntityRow = true, isExcluded = true)
            )

            val result = delegate.flattenEntities(entities, SplitType.EQUAL)

            assertEquals(1, result.size)
            assertEquals("u1", result[0].userId)
        }

        @Test
        fun `mixed solo and subunit entities`() {
            val members = listOf(makeSplit("m1", amountCents = 2000L, subunitId = "sub1"))
            val entities = listOf(
                makeSplit("solo1", amountCents = 3000L, isEntityRow = true),
                makeSplit("sub1", isEntityRow = true, entityMembers = members),
                makeSplit("solo2", amountCents = 5000L, isEntityRow = true)
            )

            val result = delegate.flattenEntities(entities, SplitType.EQUAL)

            assertEquals(3, result.size)
            assertEquals("solo1", result[0].userId)
            assertNull(result[0].subunitId)
            assertEquals("m1", result[1].userId)
            assertEquals("sub1", result[1].subunitId)
        }

        @Test
        fun `PERCENT type populates percentage on solo entities`() {
            val entities = listOf(
                makeSplit("u1", amountCents = 5000L, percentageInput = "50", isEntityRow = true)
            )

            val result = delegate.flattenEntities(entities, SplitType.PERCENT)

            assertEquals(BigDecimal("50"), result[0].percentage)
        }

        @Test
        fun `non-PERCENT type sets null percentage on solo entities`() {
            val entities = listOf(
                makeSplit("u1", amountCents = 5000L, percentageInput = "50", isEntityRow = true)
            )

            val result = delegate.flattenEntities(entities, SplitType.EXACT)

            assertNull(result[0].percentage)
        }

        @Test
        fun `empty entity list returns empty result`() {
            val result = delegate.flattenEntities(emptyList(), SplitType.EQUAL)
            assertTrue(result.isEmpty())
        }
    }

    // ── redistributePercentagesIfNeeded ──────────────────────────────────

    @Nested
    inner class RedistributePercentagesIfNeeded {

        @Test
        fun `non-PERCENT type returns unchanged`() {
            val splits = listOf(
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u1",
                    amountCents = 5000L,
                    percentage = null,
                    subunitId = null
                )
            )

            val result = delegate.redistributePercentagesIfNeeded(splits, SplitType.EQUAL)

            assertEquals(splits, result)
        }

        @Test
        fun `zero totalCents returns unchanged`() {
            val splits = listOf(
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u1",
                    amountCents = 0L,
                    percentage = null,
                    subunitId = null
                )
            )

            val result = delegate.redistributePercentagesIfNeeded(splits, SplitType.PERCENT)

            assertEquals(splits, result)
        }

        @Test
        fun `all splits have percentages returns unchanged`() {
            val splits = listOf(
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u1",
                    amountCents = 5000L,
                    percentage = BigDecimal("50"),
                    subunitId = null
                ),
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u2",
                    amountCents = 5000L,
                    percentage = BigDecimal("50"),
                    subunitId = null
                )
            )

            val result = delegate.redistributePercentagesIfNeeded(splits, SplitType.PERCENT)

            assertEquals(splits, result)
        }

        @Test
        fun `distributes remaining percentage to splits without percentage`() {
            val splits = listOf(
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u1",
                    amountCents = 5000L,
                    percentage = BigDecimal("50"),
                    subunitId = null
                ),
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u2",
                    amountCents = 3000L,
                    percentage = null,
                    subunitId = null
                ),
                es.pedrazamiguez.splittrip.domain.model.ExpenseSplit(
                    userId = "u3",
                    amountCents = 2000L,
                    percentage = null,
                    subunitId = null
                )
            )

            val result = delegate.redistributePercentagesIfNeeded(splits, SplitType.PERCENT)

            // u1 keeps 50%, u2+u3 share the remaining 50% proportionally (30+20)
            val totalPct = result.sumOf { it.percentage ?: BigDecimal.ZERO }
            assertEquals(0, totalPct.compareTo(BigDecimal("100")))
        }
    }

    // ── buildSoloSplit ───────────────────────────────────────────────────

    @Nested
    inner class BuildSoloSplit {

        @Test
        fun `PERCENT type sets percentage from percentageInput`() {
            val entity = makeSplit("u1", amountCents = 5000L, percentageInput = "50")

            val result = delegate.buildSoloSplit(entity, SplitType.PERCENT)

            assertEquals("u1", result.userId)
            assertEquals(5000L, result.amountCents)
            assertEquals(BigDecimal("50"), result.percentage)
            assertNull(result.subunitId)
        }

        @Test
        fun `non-PERCENT type sets null percentage`() {
            val entity = makeSplit("u1", amountCents = 5000L, percentageInput = "50")

            val result = delegate.buildSoloSplit(entity, SplitType.EXACT)

            assertNull(result.percentage)
        }
    }

    // ── buildMemberSplits ────────────────────────────────────────────────

    @Nested
    inner class BuildMemberSplits {

        @Test
        fun `EQUAL intra-type maps all members with splitType EQUAL and no percentage`() {
            val members = listOf(
                makeSplit("m1", amountCents = 3000L),
                makeSplit("m2", amountCents = 2000L)
            )
            val entity = makeSplit("sub1", isEntityRow = true, entityMembers = members)

            val result = delegate.buildMemberSplits(entity, SplitType.EQUAL)

            assertEquals(2, result.size)
            assertEquals("m1", result[0].userId)
            assertEquals(3000L, result[0].amountCents)
            assertEquals("sub1", result[0].subunitId)
            assertEquals(SplitType.EQUAL, result[0].splitType)
            assertNull(result[0].percentage)
            assertEquals("sub1", result[1].subunitId)
            assertEquals(SplitType.EQUAL, result[1].splitType)
        }

        @Test
        fun `EXACT intra-type maps all members with splitType EXACT and no percentage`() {
            val members = listOf(
                makeSplit("m1", amountCents = 3000L),
                makeSplit("m2", amountCents = 2000L)
            )
            val entity = makeSplit("sub1", isEntityRow = true, entityMembers = members)

            val result = delegate.buildMemberSplits(entity, SplitType.EXACT)

            result.forEach { split ->
                assertEquals(SplitType.EXACT, split.splitType)
                assertNull(split.percentage)
            }
        }

        @Test
        fun `PERCENT intra-type maps all members with splitType PERCENT and populated percentage`() {
            val members = listOf(
                makeSplit("m1", amountCents = 4250L, percentageInput = "85"),
                makeSplit("m2", amountCents = 750L, percentageInput = "15")
            )
            val entity = makeSplit("sub1", isEntityRow = true, entityMembers = members)

            val result = delegate.buildMemberSplits(entity, SplitType.PERCENT)

            assertEquals(SplitType.PERCENT, result[0].splitType)
            assertEquals(BigDecimal("85"), result[0].percentage)
            assertEquals(SplitType.PERCENT, result[1].splitType)
            assertEquals(BigDecimal("15"), result[1].percentage)
        }

        @Test
        fun `PERCENT intra-type with blank percentageInput maps percentage to null`() {
            val members = listOf(makeSplit("m1", amountCents = 5000L, percentageInput = ""))
            val entity = makeSplit("sub1", isEntityRow = true, entityMembers = members)

            val result = delegate.buildMemberSplits(entity, SplitType.PERCENT)

            assertEquals(SplitType.PERCENT, result[0].splitType)
            assertNull(result[0].percentage)
        }

        @Test
        fun `member with explicit subunitId uses that over entity userId`() {
            val members = listOf(
                makeSplit("m1", amountCents = 3000L, subunitId = "explicit-sub")
            )
            val entity = makeSplit("sub1", isEntityRow = true, entityMembers = members)

            val result = delegate.buildMemberSplits(entity, SplitType.EQUAL)

            assertEquals("explicit-sub", result[0].subunitId)
        }
    }
}
