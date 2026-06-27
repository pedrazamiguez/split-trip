package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BalancesUiMapper")
class BalancesUiMapperTest {

    private lateinit var mapper: BalancesUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    @BeforeEach
    fun setUp() {
        localeProvider = mockk()
        resourceProvider = mockk()
        every { localeProvider.getCurrentLocale() } returns Locale.US
        every { resourceProvider.getString(R.string.balances_contribution_scope_personal) } returns "Personal"
        every { resourceProvider.getString(R.string.balances_contribution_scope_group) } returns "Group"
        every { resourceProvider.getString(R.string.balances_withdraw_cash_scope_personal) } returns "Personal"
        every { resourceProvider.getString(R.string.balances_withdraw_cash_scope_group) } returns "Group"
        every {
            resourceProvider.getString(es.pedrazamiguez.splittrip.core.designsystem.R.string.user_pending_fallback)
        } returns "Pending member"
        mapper = BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
    }

    @Nested
    @DisplayName("mapActivity – merge & sort")
    inner class MapActivity {

        @Test
        fun `returns empty list when no contributions and no withdrawals`() {
            val result = mapper.mapActivity(
                contributions = emptyList(),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns contributions sorted by date descending when no withdrawals`() {
            val older = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
            )
            val newer = Contribution(
                id = "c2",
                groupId = "g1",
                userId = "u2",
                amount = 20000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 14, 0)
            )

            val result = mapper.mapActivity(
                contributions = listOf(older, newer),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(2, result.size)
            // Newest first
            assertIsContribution(result[0], "c2")
            assertIsContribution(result[1], "c1")
        }

        @Test
        fun `returns withdrawals sorted by date descending when no contributions`() {
            val older = cashWithdrawal(
                id = "cw1",
                createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
            )
            val newer = cashWithdrawal(
                id = "cw2",
                createdAt = LocalDateTime.of(2026, 1, 15, 14, 0)
            )

            val result = mapper.mapActivity(
                contributions = emptyList(),
                withdrawals = listOf(older, newer),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(2, result.size)
            assertIsWithdrawal(result[0], "cw2")
            assertIsWithdrawal(result[1], "cw1")
        }

        @Test
        fun `interleaves contributions and withdrawals sorted by date descending`() {
            val jan10Contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
            )
            val jan12Withdrawal = cashWithdrawal(
                id = "cw1",
                createdAt = LocalDateTime.of(2026, 1, 12, 8, 0)
            )
            val jan14Contribution = Contribution(
                id = "c2",
                groupId = "g1",
                userId = "u2",
                amount = 20000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 14, 14, 0)
            )
            val jan16Withdrawal = cashWithdrawal(
                id = "cw2",
                createdAt = LocalDateTime.of(2026, 1, 16, 12, 0)
            )

            val result = mapper.mapActivity(
                contributions = listOf(jan10Contribution, jan14Contribution),
                withdrawals = listOf(jan12Withdrawal, jan16Withdrawal),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(4, result.size)
            // Jan 16 withdrawal → Jan 14 contribution → Jan 12 withdrawal → Jan 10 contribution
            assertIsWithdrawal(result[0], "cw2")
            assertIsContribution(result[1], "c2")
            assertIsWithdrawal(result[2], "cw1")
            assertIsContribution(result[3], "c1")
        }

        @Test
        fun `items with null createdAt are placed at the end`() {
            val withDate = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )
            val withoutDate = Contribution(
                id = "c2",
                groupId = "g1",
                userId = "u2",
                amount = 20000,
                currency = "EUR",
                createdAt = null
            )
            val withdrawalWithoutDate = cashWithdrawal(
                id = "cw1",
                createdAt = null
            )

            val result = mapper.mapActivity(
                contributions = listOf(withDate, withoutDate),
                withdrawals = listOf(withdrawalWithoutDate),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(3, result.size)
            // Dated item first, null-dated items at the end (timestamp = 0)
            assertIsContribution(result[0], "c1")
            // Both null-dated items have timestamp 0, stable order not guaranteed
            val nullDatedIds = listOf(result[1], result[2]).map { activityId(it) }.toSet()
            assertEquals(setOf("c2", "cw1"), nullDatedIds)
        }

        @Test
        fun `sortTimestamp is correctly computed from createdAt`() {
            val dateTime = LocalDateTime.of(2026, 3, 7, 12, 30)
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                amount = 5000,
                currency = "EUR",
                createdAt = dateTime
            )

            val result = mapper.mapActivity(
                contributions = listOf(contribution),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(1, result.size)
            val item = result[0] as ActivityItemUiModel.ContributionItem
            assertTrue(item.sortTimestamp > 0)
        }

        @Test
        fun `currentUserId correctly marks isCurrentUser on contributions`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "current-user",
                amount = 5000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
            )
            val otherContribution = Contribution(
                id = "c2",
                groupId = "g1",
                userId = "other-user",
                amount = 3000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 9, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = listOf(contribution, otherContribution),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = "current-user"
            )

            val c1 = result.first { it is ActivityItemUiModel.ContributionItem && it.contribution.id == "c1" }
                as ActivityItemUiModel.ContributionItem
            val c2 = result.first { it is ActivityItemUiModel.ContributionItem && it.contribution.id == "c2" }
                as ActivityItemUiModel.ContributionItem

            assertTrue(c1.contribution.isCurrentUser)
            assertTrue(!c2.contribution.isCurrentUser)
        }

        @Test
        fun `currentUserId correctly marks isCurrentUser on withdrawals`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawnBy = "current-user",
                createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
            )
            val otherWithdrawal = cashWithdrawal(
                id = "cw2",
                withdrawnBy = "other-user",
                createdAt = LocalDateTime.of(2026, 1, 9, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = emptyList(),
                withdrawals = listOf(withdrawal, otherWithdrawal),
                groupCurrency = "EUR",
                currentUserId = "current-user"
            )

            val cw1 = result.first { it is ActivityItemUiModel.CashWithdrawalItem && it.withdrawal.id == "cw1" }
                as ActivityItemUiModel.CashWithdrawalItem
            val cw2 = result.first { it is ActivityItemUiModel.CashWithdrawalItem && it.withdrawal.id == "cw2" }
                as ActivityItemUiModel.CashWithdrawalItem

            assertTrue(cw1.withdrawal.isCurrentUser)
            assertTrue(!cw2.withdrawal.isCurrentUser)
        }

        @Test
        fun `foreign currency withdrawal shows formattedDeducted`() {
            val foreignWithdrawal = CashWithdrawal(
                id = "cw1", groupId = "g1", withdrawnBy = "u1",
                amountWithdrawn = 1000000, remainingAmount = 770000,
                currency = "THB", deductedBaseAmount = 27000,
                exchangeRate = BigDecimal("37.037"),
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = emptyList(),
                withdrawals = listOf(foreignWithdrawal),
                groupCurrency = "EUR",
                currentUserId = null
            )

            val item = result[0] as ActivityItemUiModel.CashWithdrawalItem
            assertTrue(item.withdrawal.isForeignCurrency)
            assertTrue(item.withdrawal.formattedDeducted.isNotBlank())
        }

        @Test
        fun `same currency withdrawal has empty formattedDeducted`() {
            val sameWithdrawal = cashWithdrawal(
                id = "cw1",
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = emptyList(),
                withdrawals = listOf(sameWithdrawal),
                groupCurrency = "EUR",
                currentUserId = null
            )

            val item = result[0] as ActivityItemUiModel.CashWithdrawalItem
            assertTrue(!item.withdrawal.isForeignCurrency)
            assertEquals("", item.withdrawal.formattedDeducted)
        }

        @Test
        fun `many items preserve strict descending chronological order`() {
            val contributions = (1..5).map { day ->
                Contribution(
                    id = "c$day",
                    groupId = "g1",
                    userId = "u1",
                    amount = (day * 1000).toLong(),
                    currency = "EUR",
                    createdAt = LocalDateTime.of(2026, 1, day * 2, 10, 0)
                )
            }
            val withdrawals = (1..5).map { day ->
                cashWithdrawal(
                    id = "cw$day",
                    createdAt = LocalDateTime.of(2026, 1, day * 2 + 1, 10, 0)
                )
            }

            val result = mapper.mapActivity(
                contributions = contributions,
                withdrawals = withdrawals,
                groupCurrency = "EUR",
                currentUserId = null
            )

            assertEquals(10, result.size)

            // Verify strictly descending timestamps
            for (i in 0 until result.size - 1) {
                assertTrue(
                    result[i].sortTimestamp >= result[i + 1].sortTimestamp,
                    "Item at index $i (ts=${result[i].sortTimestamp}) should be >= " +
                        "item at index ${i + 1} (ts=${result[i + 1].sortTimestamp})"
                )
            }
        }
    }

    @Nested
    @DisplayName("mapContributions – contribution scope")
    inner class ContributionScope {

        private val testSubunit = Subunit(
            id = "subunit-1",
            groupId = "g1",
            name = "Antonio & Me",
            memberIds = listOf("u1", "u2")
        )
        private val subunitsMap = mapOf("subunit-1" to testSubunit)

        @Test
        fun `SUBUNIT-scoped contribution resolves subunit name as scopeLabel`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Antonio & Me", result[0].scopeLabel)
            assertTrue(result[0].isSubunitContribution)
            assertFalse(result[0].isPersonalContribution)
            assertFalse(result[0].isGroupContribution)
        }

        @Test
        fun `USER-scoped contribution has Personal as scopeLabel`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.USER,
                subunitId = null,
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Personal", result[0].scopeLabel)
            assertFalse(result[0].isSubunitContribution)
            assertTrue(result[0].isPersonalContribution)
            assertFalse(result[0].isGroupContribution)
        }

        @Test
        fun `GROUP-scoped contribution has Group as scopeLabel`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.GROUP,
                subunitId = null,
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Group", result[0].scopeLabel)
            assertFalse(result[0].isSubunitContribution)
            assertFalse(result[0].isPersonalContribution)
            assertTrue(result[0].isGroupContribution)
        }

        @Test
        fun `SUBUNIT-scoped contribution with unknown subunitId has null scopeLabel`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.SUBUNIT,
                subunitId = "nonexistent",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals(null, result[0].scopeLabel)
            assertTrue(result[0].isSubunitContribution)
        }

        @Test
        fun `mapActivity passes scope fields through to contribution items`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = listOf(contribution),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            val item = result[0] as ActivityItemUiModel.ContributionItem
            assertEquals("Antonio & Me", item.contribution.scopeLabel)
            assertTrue(item.contribution.isSubunitContribution)
        }
    }

    @Nested
    @DisplayName("mapCashWithdrawals – withdrawal scope")
    inner class CashWithdrawalScope {

        private val subunitsMap = mapOf(
            "subunit-1" to Subunit(
                id = "subunit-1",
                name = "Antonio & Me",
                groupId = "g1",
                memberIds = listOf("u1", "u2")
            )
        )

        @Test
        fun `GROUP-scoped withdrawal has Group as scopeLabel`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawalScope = PayerType.GROUP,
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Group", result[0].scopeLabel)
            assertEquals(false, result[0].isSubunitWithdrawal)
            assertEquals(false, result[0].isPersonalWithdrawal)
            assertEquals(true, result[0].isGroupWithdrawal)
        }

        @Test
        fun `SUBUNIT-scoped withdrawal has subunit name as scopeLabel`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Antonio & Me", result[0].scopeLabel)
            assertEquals(true, result[0].isSubunitWithdrawal)
            assertEquals(false, result[0].isPersonalWithdrawal)
        }

        @Test
        fun `USER-scoped withdrawal has Personal as scopeLabel`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawalScope = PayerType.USER,
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals("Personal", result[0].scopeLabel)
            assertEquals(false, result[0].isSubunitWithdrawal)
            assertEquals(true, result[0].isPersonalWithdrawal)
        }

        @Test
        fun `SUBUNIT-scoped withdrawal with unknown subunitId has null scopeLabel`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "nonexistent",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1",
                subunits = subunitsMap
            )

            assertEquals(1, result.size)
            assertEquals(null, result[0].scopeLabel)
            assertEquals(true, result[0].isSubunitWithdrawal)
        }
    }

    @Nested
    @DisplayName("mapCashWithdrawals – title and notes metadata")
    inner class CashWithdrawalMetadata {

        @Test
        fun `maps title and notes when present`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                title = "Airport ATM",
                notes = "Bad rate but no other option",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            assertEquals("Airport ATM", result[0].title)
            assertEquals("Bad rate but no other option", result[0].notes)
        }

        @Test
        fun `maps null title and notes when absent`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            assertNull(result[0].title)
            assertNull(result[0].notes)
        }

        @Test
        fun `maps title without notes`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                title = "Hotel exchange desk",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            assertEquals("Hotel exchange desk", result[0].title)
            assertNull(result[0].notes)
        }
    }

    @Nested
    @DisplayName("createdByDisplayName – on-behalf-of resolution")
    inner class CreatedByDisplay {

        private val actorProfile = User(userId = "actor-id", email = "actor@test.com", displayName = "Andrés")
        private val memberProfiles = mapOf("actor-id" to actorProfile)

        @Test
        fun `contribution createdByDisplayName is null when createdBy equals userId`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "actor-id",
                createdBy = "actor-id",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "actor-id",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }

        @Test
        fun `contribution createdByDisplayName resolves actor name when createdBy differs from userId`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "target-user",
                createdBy = "actor-id",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertEquals("Andrés", result[0].createdByDisplayName)
        }

        @Test
        fun `contribution createdByDisplayName is null when createdBy is blank`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "target-user",
                createdBy = "",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }

        @Test
        fun `contribution createdByDisplayName is null when actor profile is missing from memberProfiles`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "target-user",
                createdBy = "unknown-actor",
                amount = 10000,
                currency = "EUR",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }

        @Test
        fun `withdrawal createdByDisplayName is null when createdBy equals withdrawnBy`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawnBy = "actor-id",
                createdBy = "actor-id",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "actor-id",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }

        @Test
        fun `withdrawal createdByDisplayName resolves actor name when createdBy differs from withdrawnBy`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawnBy = "target-user",
                createdBy = "actor-id",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertEquals("Andrés", result[0].createdByDisplayName)
        }

        @Test
        fun `withdrawal createdByDisplayName is null when actor profile is missing from memberProfiles`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawnBy = "target-user",
                createdBy = "unknown-actor",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }

        @Test
        fun `withdrawal createdByDisplayName is null when createdBy is blank`() {
            val withdrawal = cashWithdrawal(
                id = "cw1",
                withdrawnBy = "target-user",
                createdBy = "",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "target-user",
                memberProfiles = memberProfiles
            )

            assertNull(result[0].createdByDisplayName)
        }
    }

    @Nested
    @DisplayName("mapContributions – linked contributions (out-of-pocket)")
    inner class LinkedContributions {

        @Test
        fun `isLinkedContribution is true when linkedExpenseId is non-null`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.USER,
                amount = 16500,
                currency = "EUR",
                linkedExpenseId = "exp-1",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            assertTrue(result[0].isLinkedContribution)
        }

        @Test
        fun `isLinkedContribution is false when linkedExpenseId is null`() {
            val contribution = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.USER,
                amount = 10000,
                currency = "EUR",
                linkedExpenseId = null,
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            assertFalse(result[0].isLinkedContribution)
        }

        @Test
        fun `mapActivity passes isLinkedContribution through to contribution items`() {
            val linked = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.USER,
                amount = 16500,
                currency = "EUR",
                linkedExpenseId = "exp-1",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapActivity(
                contributions = listOf(linked),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                currentUserId = "u1"
            )

            assertEquals(1, result.size)
            val item = result[0] as ActivityItemUiModel.ContributionItem
            assertTrue(item.contribution.isLinkedContribution)
        }

        @Test
        fun `linked and manual contributions coexist in same list`() {
            val manual = Contribution(
                id = "c1",
                groupId = "g1",
                userId = "u1",
                contributionScope = PayerType.GROUP,
                amount = 10000,
                currency = "EUR",
                linkedExpenseId = null,
                createdAt = LocalDateTime.of(2026, 1, 14, 10, 0)
            )
            val linked = Contribution(
                id = "c2",
                groupId = "g1",
                userId = "u2",
                contributionScope = PayerType.USER,
                amount = 16500,
                currency = "EUR",
                linkedExpenseId = "exp-1",
                createdAt = LocalDateTime.of(2026, 1, 15, 10, 0)
            )

            val result = mapper.mapContributions(
                contributions = listOf(manual, linked),
                currentUserId = "u1"
            )

            assertEquals(2, result.size)
            assertFalse(result[0].isLinkedContribution)
            assertTrue(result[1].isLinkedContribution)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun cashWithdrawal(
        id: String,
        groupId: String = "g1",
        withdrawnBy: String = "u1",
        createdBy: String = "",
        withdrawalScope: PayerType = PayerType.GROUP,
        subunitId: String? = null,
        amountWithdrawn: Long = 100000,
        remainingAmount: Long = 100000,
        currency: String = "THB",
        deductedBaseAmount: Long = 27000,
        exchangeRate: BigDecimal = BigDecimal("37.037"),
        title: String? = null,
        notes: String? = null,
        createdAt: LocalDateTime? = null,
        addOns: List<AddOn> = emptyList()
    ) = CashWithdrawal(
        id = id,
        groupId = groupId,
        withdrawnBy = withdrawnBy,
        createdBy = createdBy,
        withdrawalScope = withdrawalScope,
        subunitId = subunitId,
        amountWithdrawn = amountWithdrawn,
        remainingAmount = remainingAmount,
        currency = currency,
        deductedBaseAmount = deductedBaseAmount,
        exchangeRate = exchangeRate,
        title = title,
        notes = notes,
        createdAt = createdAt,
        addOns = addOns
    )

    private fun assertIsContribution(item: ActivityItemUiModel, expectedId: String) {
        assertTrue(
            item is ActivityItemUiModel.ContributionItem,
            "Expected ContributionItem but got ${item::class.simpleName}"
        )
        assertEquals(expectedId, (item as ActivityItemUiModel.ContributionItem).contribution.id)
    }

    private fun assertIsWithdrawal(item: ActivityItemUiModel, expectedId: String) {
        assertTrue(
            item is ActivityItemUiModel.CashWithdrawalItem,
            "Expected CashWithdrawalItem but got ${item::class.simpleName}"
        )
        assertEquals(expectedId, (item as ActivityItemUiModel.CashWithdrawalItem).withdrawal.id)
    }

    private fun activityId(item: ActivityItemUiModel): String = when (item) {
        is ActivityItemUiModel.ContributionItem -> item.contribution.id
        is ActivityItemUiModel.CashWithdrawalItem -> item.withdrawal.id
    }

    @Nested
    @DisplayName("SyncStatus mapping")
    inner class SyncStatusMapping {

        @Test
        fun `contribution maps PENDING_SYNC status`() {
            val contribution = Contribution(
                id = "c-sync",
                amount = 1000L,
                currency = "EUR",
                syncStatus = SyncStatus.PENDING_SYNC
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = emptyMap()
            )

            assertEquals(SyncStatus.PENDING_SYNC, result[0].syncStatus)
        }

        @Test
        fun `contribution maps SYNCED status by default`() {
            val contribution = Contribution(
                id = "c-sync-default",
                amount = 1000L,
                currency = "EUR"
            )

            val result = mapper.mapContributions(
                contributions = listOf(contribution),
                currentUserId = "u1",
                subunits = emptyMap()
            )

            assertEquals(SyncStatus.SYNCED, result[0].syncStatus)
        }

        @Test
        fun `withdrawal maps SYNC_FAILED status`() {
            val withdrawal = CashWithdrawal(
                id = "w-sync",
                amountWithdrawn = 5000L,
                currency = "THB",
                syncStatus = SyncStatus.SYNC_FAILED
            )

            val result = mapper.mapCashWithdrawals(
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                currentUserId = "u1"
            )

            assertEquals(SyncStatus.SYNC_FAILED, result[0].syncStatus)
        }
    }

    @Nested
    @DisplayName("mapExtrasBreakdown")
    inner class MapExtrasBreakdown {

        @BeforeEach
        fun setUpMocks() {
            every { resourceProvider.getString(R.string.balances_extras_expense_fallback) } returns "Expense"
            every { resourceProvider.getString(eq(R.string.balances_extras_atm_fallback), any()) } answers {
                val date = (args[1] as Array<*>)[0] as String
                "ATM — $date"
            }
            every { resourceProvider.getString(R.string.balances_extras_add_on_fee_plural) } returns "Fees"
            every { resourceProvider.getString(R.string.balances_extras_add_on_surcharge_plural) } returns "Surcharges"
            every { resourceProvider.getString(R.string.balances_extras_add_on_tip_plural) } returns "Tips"
            every { resourceProvider.getString(R.string.balances_member_you) } returns "You"
            every {
                resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)
            } returns "Unknown subunit"
            every { resourceProvider.getString(R.string.balances_contribution_scope_group) } returns "Group"
        }

        @Test
        fun `returns empty list when expenses and withdrawals have no add-ons`() {
            val result = mapper.mapExtrasBreakdown(
                expenses = emptyList(),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when all add-ons are DISCOUNT type`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(AddOn(type = AddOnType.DISCOUNT, groupAmountCents = 100))
            )
            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )
            assertTrue(result.isEmpty())
        }

        @Test
        fun `maps single expense add-on to correct type group and scope`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 150)),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )
            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertEquals("Fees", result[0].typeLabel)
            assertTrue(result[0].formattedSubtotal.contains("1.50"))
            assertEquals(1, result[0].items.size)
            assertEquals("Group", result[0].items[0].parentTitle)
            assertTrue(result[0].items[0].formattedAmount.contains("1.50"))
        }

        @Test
        fun `maps single withdrawal add-on to correct type group and scope`() {
            val withdrawal = CashWithdrawal(
                id = "w1",
                title = "ATM 1",
                addOns = listOf(AddOn(type = AddOnType.TIP, groupAmountCents = 200)),
                createdAt = LocalDateTime.of(2026, 1, 12, 10, 0)
            )
            val result = mapper.mapExtrasBreakdown(
                expenses = emptyList(),
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertEquals("Tips", result[0].typeLabel)
            assertTrue(result[0].formattedSubtotal.contains("2.00"))
            assertEquals("Group", result[0].items[0].parentTitle)
        }

        @Test
        fun `groups multiple add-ons of same type by scope and sums amount`() {
            val expense1 = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 150)),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )
            val expense2 = Expense(
                id = "e2",
                title = "Taxi",
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 250)),
                createdAt = LocalDateTime.of(2026, 1, 11, 12, 0)
            )

            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense1, expense2),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertEquals("Fees", result[0].typeLabel)
            assertTrue(result[0].formattedSubtotal.contains("4.00"))
            assertEquals(1, result[0].items.size)
            assertEquals("Group", result[0].items[0].parentTitle)
            assertTrue(result[0].items[0].formattedAmount.contains("4.00"))
        }

        @Test
        fun `produces separate sections for TIP FEE and SURCHARGE`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(
                    AddOn(type = AddOnType.FEE, groupAmountCents = 100),
                    AddOn(type = AddOnType.SURCHARGE, groupAmountCents = 200),
                    AddOn(type = AddOnType.TIP, groupAmountCents = 300)
                ),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )

            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(3, result.size)
            assertEquals("Fees", result[0].typeLabel)
            assertEquals("Surcharges", result[1].typeLabel)
            assertEquals("Tips", result[2].typeLabel)
        }

        @Test
        fun `section order is FEE then SURCHARGE then TIP`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(
                    AddOn(type = AddOnType.TIP, groupAmountCents = 300),
                    AddOn(type = AddOnType.FEE, groupAmountCents = 100),
                    AddOn(type = AddOnType.SURCHARGE, groupAmountCents = 200)
                ),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )

            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(3, result.size)
            assertEquals("Fees", result[0].typeLabel)
            assertEquals("Surcharges", result[1].typeLabel)
            assertEquals("Tips", result[2].typeLabel)
        }

        @Test
        fun `sub-total per section is correct sum of groupAmountCents`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(
                    AddOn(type = AddOnType.FEE, groupAmountCents = 100),
                    AddOn(type = AddOnType.FEE, groupAmountCents = 150)
                ),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )

            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = emptyList(),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertTrue(result[0].formattedSubtotal.contains("2.50"))
        }

        @Test
        fun `mixes expense and withdrawal add-ons into same type bucket and groups by scope`() {
            val expense = Expense(
                id = "e1",
                title = "Dinner",
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 100)),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )
            val withdrawal = CashWithdrawal(
                id = "w1",
                title = "ATM 1",
                addOns = listOf(AddOn(type = AddOnType.FEE, groupAmountCents = 200)),
                createdAt = LocalDateTime.of(2026, 1, 11, 12, 0)
            )

            val result = mapper.mapExtrasBreakdown(
                expenses = listOf(expense),
                withdrawals = listOf(withdrawal),
                groupCurrency = "EUR",
                memberProfiles = emptyMap(),
                subunitsMap = emptyMap(),
                currentUserId = null
            )

            assertEquals(1, result.size)
            assertEquals("Fees", result[0].typeLabel)
            assertTrue(result[0].formattedSubtotal.contains("3.00"))
            assertEquals(1, result[0].items.size)
            assertEquals("Group", result[0].items[0].parentTitle)
            assertTrue(result[0].items[0].formattedAmount.contains("3.00"))
        }
    }
}
