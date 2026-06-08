package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.impl.RemainderDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.EntitySplitFlattenDelegate
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddExpenseSplitUiMapperTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var splitPreviewService: SplitPreviewService
    private lateinit var entitySplitFlattenDelegate: EntitySplitFlattenDelegate
    private lateinit var mapper: AddExpenseSplitUiMapper

    private val user1 = User(userId = "user-1", email = "a@test.com", displayName = "Andrés")
    private val user2 = User(userId = "user-2", email = "b@test.com", displayName = "Ana")
    private val user3 = User(userId = "user-3", email = "c@test.com", displayName = "")

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        formattingHelper = FormattingHelper(localeProvider)
        splitPreviewService = SplitPreviewServiceImpl()
        val remainderDistributionService = RemainderDistributionServiceImpl()
        entitySplitFlattenDelegate = EntitySplitFlattenDelegate(splitPreviewService, remainderDistributionService)

        mapper = AddExpenseSplitUiMapper(
            localeProvider,
            formattingHelper,
            splitPreviewService,
            entitySplitFlattenDelegate
        )
    }

    // ── resolveDisplayName ────────────────────────────────────────────────

    @Nested
    inner class ResolveDisplayName {

        @Test
        fun `returns displayName when set`() {
            val profiles = mapOf("user-1" to user1)
            assertEquals("Andrés", mapper.resolveDisplayName("user-1", profiles))
        }

        @Test
        fun `returns email as fallback when displayName is blank`() {
            val profiles = mapOf("user-3" to user3)
            assertEquals("c@test.com", mapper.resolveDisplayName("user-3", profiles))
        }

        @Test
        fun `returns userId as fallback when profile is not found`() {
            assertEquals("user-99", mapper.resolveDisplayName("user-99", emptyMap()))
        }

        @Test
        fun `returns userId when user has no displayName and no email`() {
            val noNameNoEmail = User(userId = "user-x", email = "", displayName = "")
            val profiles = mapOf("user-x" to noNameNoEmail)
            assertEquals("user-x", mapper.resolveDisplayName("user-x", profiles))
        }
    }

    // ── buildInitialSplits ────────────────────────────────────────────────

    @Nested
    inner class BuildInitialSplits {

        @Test
        fun `builds splits from member IDs with existing shares`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 5000L),
                ExpenseSplit(userId = "user-2", amountCents = 5000L)
            )

            val result = mapper.buildInitialSplits(
                memberIds = listOf("user-1", "user-2"),
                shares = shares,
                memberProfiles = mapOf("user-1" to user1, "user-2" to user2)
            )

            assertEquals(2, result.size)
            // Results are sorted by display name (Ana < Andrés in locale sort)
            val names = result.map { it.displayName }
            assertTrue(names.contains("Andrés"))
            assertTrue(names.contains("Ana"))
        }

        @Test
        fun `uses zero amountCents when member has no share`() {
            val result = mapper.buildInitialSplits(
                memberIds = listOf("user-1"),
                shares = emptyList(),
                memberProfiles = mapOf("user-1" to user1)
            )

            assertEquals(1, result.size)
            assertEquals(0L, result[0].amountCents)
        }

        @Test
        fun `returns empty list for empty member IDs`() {
            val result = mapper.buildInitialSplits(
                memberIds = emptyList(),
                shares = emptyList()
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `sorts results by display name`() {
            val result = mapper.buildInitialSplits(
                memberIds = listOf("user-1", "user-2"),
                shares = emptyList(),
                memberProfiles = mapOf("user-1" to user1, "user-2" to user2)
            )

            // Ana < Andrés alphabetically
            assertEquals("Ana", result[0].displayName)
            assertEquals("Andrés", result[1].displayName)
        }

        @Test
        fun `populates percentageInput from share percentage`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 5000L, percentage = BigDecimal("50.00"))
            )

            val result = mapper.buildInitialSplits(
                memberIds = listOf("user-1"),
                shares = shares
            )

            assertEquals("50.00", result[0].percentageInput)
        }

        @Test
        fun `percentageInput is blank when share has no percentage`() {
            val shares = listOf(ExpenseSplit(userId = "user-1", amountCents = 5000L, percentage = null))

            val result = mapper.buildInitialSplits(
                memberIds = listOf("user-1"),
                shares = shares
            )

            assertEquals("", result[0].percentageInput)
        }
    }

    // ── mapSplitsToDomain ─────────────────────────────────────────────────

    @Nested
    inner class MapSplitsToDomain {

        @Test
        fun `maps splits without percentages in EQUAL mode`() {
            val splits = listOf(
                SplitUiModel(userId = "user-1", displayName = "Andrés", amountCents = 5000L),
                SplitUiModel(userId = "user-2", displayName = "Ana", amountCents = 5000L)
            )

            val result = mapper.mapSplitsToDomain(splits, SplitType.EQUAL)

            assertEquals(2, result.size)
            assertNull(result[0].percentage)
            assertNull(result[1].percentage)
        }

        @Test
        fun `maps splits with percentages in PERCENT mode`() {
            val splits = listOf(
                SplitUiModel(
                    userId = "user-1",
                    displayName = "Andrés",
                    amountCents = 6000L,
                    percentageInput = "60.00"
                )
            )

            val result = mapper.mapSplitsToDomain(splits, SplitType.PERCENT)

            assertEquals(1, result.size)
            assertNotNull(result[0].percentage)
        }

        @Test
        fun `excludes splits marked as excluded`() {
            val splits = listOf(
                SplitUiModel(userId = "user-1", displayName = "Andrés", amountCents = 10000L),
                SplitUiModel(userId = "user-2", displayName = "Ana", amountCents = 0L, isExcluded = true)
            )

            val result = mapper.mapSplitsToDomain(splits, SplitType.EQUAL)

            assertEquals(1, result.size)
            assertEquals("user-1", result[0].userId)
        }

        @Test
        fun `returns empty list when all splits are excluded`() {
            val splits = listOf(
                SplitUiModel(userId = "user-1", displayName = "Andrés", isExcluded = true)
            )

            val result = mapper.mapSplitsToDomain(splits, SplitType.EQUAL)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `preserves subunitId on each split`() {
            val splits = listOf(
                SplitUiModel(
                    userId = "user-1",
                    displayName = "Andrés",
                    amountCents = 5000L,
                    subunitId = "subunit-1"
                )
            )

            val result = mapper.mapSplitsToDomain(splits, SplitType.EQUAL)

            assertEquals("subunit-1", result[0].subunitId)
        }
    }

    // ── mapDomainToSplits ─────────────────────────────────────────────────

    @Nested
    inner class MapDomainToSplits {

        @Test
        fun `maps domain splits back to UI models`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 5000L),
                ExpenseSplit(userId = "user-2", amountCents = 5000L)
            )

            val result = mapper.mapDomainToSplits(
                memberIds = listOf("user-1", "user-2"),
                shares = shares,
                memberProfiles = mapOf("user-1" to user1, "user-2" to user2)
            )

            assertEquals(2, result.size)
            val ids = result.map { it.userId }
            assertTrue(ids.contains("user-1"))
            assertTrue(ids.contains("user-2"))
        }

        @Test
        fun `marks member as excluded when not in domain splits`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 10000L)
                // user-2 is NOT in shares → excluded
            )

            val result = mapper.mapDomainToSplits(
                memberIds = listOf("user-1", "user-2"),
                shares = shares
            )

            val user2Row = result.first { it.userId == "user-2" }
            assertTrue(user2Row.isExcluded)
        }

        @Test
        fun `marks member as not excluded when in domain splits`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 10000L)
            )

            val result = mapper.mapDomainToSplits(
                memberIds = listOf("user-1"),
                shares = shares
            )

            assertFalse(result[0].isExcluded)
        }

        @Test
        fun `returns empty list for empty member IDs`() {
            val result = mapper.mapDomainToSplits(
                memberIds = emptyList(),
                shares = emptyList()
            )

            assertTrue(result.isEmpty())
        }
    }

    // ── mapEntitySplitsToDomain ───────────────────────────────────────────

    @Nested
    inner class MapEntitySplitsToDomain {

        @Test
        fun `maps flat entity splits to domain splits in EQUAL mode`() {
            val entitySplits = listOf(
                SplitUiModel(
                    userId = "user-1",
                    displayName = "Andrés",
                    amountCents = 5000L,
                    isEntityRow = true
                )
            )

            val result = mapper.mapEntitySplitsToDomain(entitySplits, SplitType.EQUAL)

            assertTrue(result.isNotEmpty())
        }

        @Test
        fun `returns empty list for empty entity splits`() {
            val result = mapper.mapEntitySplitsToDomain(emptyList(), SplitType.EQUAL)
            assertTrue(result.isEmpty())
        }
    }

    // ── buildEntitySplitsFromDomain ───────────────────────────────────────

    @Nested
    inner class BuildEntitySplitsFromDomain {

        private val subunit = Subunit(
            id = "sub-1",
            groupId = "group-1",
            name = "Couple",
            memberIds = listOf("user-1", "user-2"),
            memberShares = mapOf("user-1" to BigDecimal("50"), "user-2" to BigDecimal("50"))
        )

        private val availableSplitTypes = listOf(
            SplitTypeUiModel(id = "EQUAL", displayText = "Equal"),
            SplitTypeUiModel(id = "EXACT", displayText = "Exact"),
            SplitTypeUiModel(id = "PERCENT", displayText = "Percent")
        )

        @Test
        fun `builds entity rows for subunits`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 3000L, subunitId = "sub-1"),
                ExpenseSplit(userId = "user-2", amountCents = 3000L, subunitId = "sub-1")
            )

            val result = mapper.buildEntitySplitsFromDomain(
                memberIds = listOf("user-1", "user-2"),
                subunits = listOf(subunit),
                shares = shares,
                availableSplitTypes = availableSplitTypes,
                memberProfiles = mapOf("user-1" to user1, "user-2" to user2)
            )

            // Should have one entity row for the subunit
            val subunitRow = result.firstOrNull { it.isEntityRow && it.userId == "sub-1" }
            assertNotNull(subunitRow)
            assertEquals("Couple", subunitRow!!.displayName)
            assertEquals(2, subunitRow.entityMembers.size)
        }

        @Test
        fun `marks subunit as excluded when no shares exist for it`() {
            val result = mapper.buildEntitySplitsFromDomain(
                memberIds = listOf("user-1", "user-2"),
                subunits = listOf(subunit),
                shares = emptyList(), // no shares → subunit excluded
                availableSplitTypes = availableSplitTypes
            )

            val subunitRow = result.firstOrNull { it.isEntityRow && it.userId == "sub-1" }
            assertNotNull(subunitRow)
            assertTrue(subunitRow!!.isExcluded)
        }

        @Test
        fun `builds solo member rows for members not in any subunit`() {
            val shares = listOf(
                ExpenseSplit(userId = "user-3", amountCents = 5000L)
            )

            val soloUser = User(userId = "user-3", email = "solo@test.com", displayName = "Solo")

            val result = mapper.buildEntitySplitsFromDomain(
                memberIds = listOf("user-1", "user-2", "user-3"),
                subunits = listOf(subunit), // only user-1 and user-2 are in subunit
                shares = shares,
                availableSplitTypes = availableSplitTypes,
                memberProfiles = mapOf("user-3" to soloUser)
            )

            val soloRow = result.firstOrNull { it.isEntityRow && it.userId == "user-3" }
            assertNotNull(soloRow)
        }

        @Test
        fun `returns empty list when no members and no subunits`() {
            val result = mapper.buildEntitySplitsFromDomain(
                memberIds = emptyList(),
                subunits = emptyList(),
                shares = emptyList(),
                availableSplitTypes = availableSplitTypes
            )

            assertTrue(result.isEmpty())
        }
    }
}
