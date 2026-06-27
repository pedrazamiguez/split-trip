package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceCashContext
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BalancesUiMapper — Member Balances, Balance, & Formatting")
class BalancesUiMapperMemberBalancesTest {

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
        every { resourceProvider.getString(R.string.balances_cash_breakdown_group_scope) } returns
            "Group cash (est. share)"
        every { resourceProvider.getString(R.string.balances_cash_breakdown_personal_scope) } returns "Personal cash"
        every { resourceProvider.getString(R.string.balances_cash_breakdown_atm_fallback, any()) } returns
            "ATM — Jan 10"
        every { resourceProvider.getString(R.string.balances_cash_breakdown_rate, any(), any(), any()) } returns
            "@ 0.027 THB/EUR"
        every {
            resourceProvider.getString(es.pedrazamiguez.splittrip.core.designsystem.R.string.user_pending_fallback)
        } returns "Pending member"
        mapper = BalancesUiMapper(localeProvider, resourceProvider, UserUiMapper(resourceProvider))
    }

    @Nested
    @DisplayName("mapMemberBalances")
    inner class MapMemberBalances {

        private val currency = "EUR"
        private val currentUserId = "user-1"
        private val memberProfiles = mapOf(
            "user-1" to User(userId = "user-1", email = "alice@test.com", displayName = "Alice"),
            "user-2" to User(userId = "user-2", email = "bob@test.com", displayName = "Bob"),
            "user-3" to User(userId = "user-3", email = "charlie@test.com")
        )

        @Test
        fun `returns empty list when no balances`() {
            val result = mapper.mapMemberBalances(
                balances = emptyList(),
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `formats all amount fields correctly`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    contributed = 5000L,
                    withdrawn = 3000L,
                    cashSpent = 500L,
                    nonCashSpent = 500L,
                    totalSpent = 1000L,
                    pocketBalance = 1500L,
                    cashInHand = 2500L
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            assertEquals(1, result.size)
            val item = result[0]
            // US locale EUR formatting: €50.00 (contributed), €25.00 (cashInHand), €10.00 (spent), €15.00 (pocket)
            assertTrue(item.formattedContributed.contains("50"))
            assertTrue(item.formattedCashInHand.contains("25"))
            assertTrue(item.formattedTotalSpent.contains("10"))
            assertTrue(item.formattedPocketBalance.contains("15"))
        }

        @Test
        fun `resolves display name from profiles`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 0L),
                MemberBalance(userId = "user-2", pocketBalance = 0L),
                MemberBalance(userId = "user-3", pocketBalance = 0L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            val byUser = result.associateBy { it.userId }
            assertEquals("Alice", byUser["user-1"]!!.displayName)
            assertEquals("Bob", byUser["user-2"]!!.displayName)
            // user-3 has no displayName, falls back to email
            assertEquals("charlie@test.com", byUser["user-3"]!!.displayName)
        }

        @Test
        fun `marks current user correctly`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 100L),
                MemberBalance(userId = "user-2", pocketBalance = -100L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = "user-1",
                memberProfiles = memberProfiles
            )

            assertTrue(result[0].isCurrentUser)
            assertFalse(result[1].isCurrentUser)
        }

        @Test
        fun `current user is sorted first`() {
            val balances = listOf(
                MemberBalance(userId = "user-2", pocketBalance = -5000L),
                MemberBalance(userId = "user-1", pocketBalance = 100L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = "user-1",
                memberProfiles = memberProfiles
            )

            assertEquals("user-1", result[0].userId)
            assertEquals("user-2", result[1].userId)
        }

        @Test
        fun `members sorted by absolute pocketBalance descending after current user`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 100L),
                MemberBalance(userId = "user-2", pocketBalance = -5000L),
                MemberBalance(userId = "user-3", pocketBalance = 3000L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = "user-1",
                memberProfiles = memberProfiles
            )

            // user-1 first (current user), then user-2 (|5000|), then user-3 (|3000|)
            assertEquals("user-1", result[0].userId)
            assertEquals("user-2", result[1].userId)
            assertEquals("user-3", result[2].userId)
        }

        @Test
        fun `positive balance flagged correctly`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 1000L),
                MemberBalance(userId = "user-2", pocketBalance = -500L),
                MemberBalance(userId = "user-3", pocketBalance = 0L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = null,
                memberProfiles = memberProfiles
            )

            val byUser = result.associateBy { it.userId }
            assertTrue(byUser["user-1"]!!.isPositiveBalance)
            assertFalse(byUser["user-2"]!!.isPositiveBalance)
            assertTrue(byUser["user-3"]!!.isPositiveBalance) // zero is positive
        }

        @Test
        fun `formats cashSpent and nonCashSpent fields`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    cashSpent = 1500L,
                    nonCashSpent = 2500L,
                    totalSpent = 4000L,
                    pocketBalance = 1000L
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            val item = result[0]
            assertTrue(item.formattedCashSpent.contains("15"))
            assertTrue(item.formattedNonCashSpent.contains("25"))
        }

        @Test
        fun `maps currency breakdown with foreign equivalent`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    pocketBalance = 1000L,
                    cashInHandByCurrency = listOf(
                        CurrencyAmount(currency = "THB", amountCents = 50000L, equivalentCents = 1342L)
                    )
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrency = "EUR"
            )

            val item = result[0]
            assertEquals(1, item.cashInHandByCurrency.size)
            val thb = item.cashInHandByCurrency[0]
            assertEquals("THB", thb.currency)
            assertTrue(thb.formattedAmount.isNotBlank())
            assertTrue(thb.formattedEquivalent.isNotBlank()) // foreign → shows equivalent
        }

        @Test
        fun `currency breakdown equivalent is empty for group currency`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    pocketBalance = 1000L,
                    cashInHandByCurrency = listOf(
                        CurrencyAmount(currency = "EUR", amountCents = 5000L, equivalentCents = 5000L)
                    )
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrency = "EUR"
            )

            val item = result[0]
            assertEquals(1, item.cashInHandByCurrency.size)
            assertEquals("", item.cashInHandByCurrency[0].formattedEquivalent) // same currency → empty
        }

        @Test
        fun `empty per-currency lists produce empty ImmutableLists`() {
            val balances = listOf(
                MemberBalance(userId = "user-1", pocketBalance = 0L)
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrency = "EUR"
            )

            val item = result[0]
            assertTrue(item.cashInHandByCurrency.isEmpty())
            assertTrue(item.cashSpentByCurrency.isEmpty())
            assertTrue(item.nonCashSpentByCurrency.isEmpty())
        }

        @Test
        fun `maps all three per-currency breakdown lists`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    pocketBalance = 1000L,
                    cashInHandByCurrency = listOf(
                        CurrencyAmount(currency = "THB", amountCents = 50000L, equivalentCents = 1342L)
                    ),
                    cashSpentByCurrency = listOf(
                        CurrencyAmount(currency = "THB", amountCents = 4500L, equivalentCents = 121L)
                    ),
                    nonCashSpentByCurrency = listOf(
                        CurrencyAmount(currency = "EUR", amountCents = 1000L, equivalentCents = 1000L),
                        CurrencyAmount(currency = "THB", amountCents = 20000L, equivalentCents = 540L)
                    )
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrency = "EUR"
            )

            val item = result[0]
            assertEquals(1, item.cashInHandByCurrency.size)
            assertEquals(1, item.cashSpentByCurrency.size)
            assertEquals(2, item.nonCashSpentByCurrency.size)
        }

        @Test
        fun `negative cashInHand shows em dash and sets flag`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    cashInHand = -7500L,
                    cashSpent = 7500L,
                    pocketBalance = 5000L
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            val item = result[0]
            assertEquals(BalancesUiMapper.EM_DASH, item.formattedCashInHand)
            assertTrue(item.hasNegativeCashInHand)
        }

        @Test
        fun `negative cashInHand suppresses per-currency breakdown`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    cashInHand = -2000L,
                    cashSpent = 9000L,
                    pocketBalance = 1000L,
                    cashInHandByCurrency = listOf(
                        CurrencyAmount(currency = "THB", amountCents = 30000L, equivalentCents = 800L)
                    )
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles,
                groupCurrency = "EUR"
            )

            val item = result[0]
            assertTrue(item.hasNegativeCashInHand)
            assertTrue(item.cashInHandByCurrency.isEmpty())
        }

        @Test
        fun `zero cashInHand with no withdrawals keeps default flag false`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    cashInHand = 0L,
                    withdrawn = 0L,
                    pocketBalance = 0L
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            val item = result[0]
            assertFalse(item.hasNegativeCashInHand)
            assertTrue(item.formattedCashInHand.contains("0"))
        }

        @Test
        fun `positive cashInHand keeps default flag false`() {
            val balances = listOf(
                MemberBalance(
                    userId = "user-1",
                    cashInHand = 2500L,
                    pocketBalance = 1000L
                )
            )

            val result = mapper.mapMemberBalances(
                balances = balances,
                currency = currency,
                currentUserId = currentUserId,
                memberProfiles = memberProfiles
            )

            val item = result[0]
            assertFalse(item.hasNegativeCashInHand)
            assertTrue(item.formattedCashInHand.contains("25"))
        }
    }

    @Nested
    @DisplayName("mapBalance – GroupPocketBalance → GroupPocketBalanceUiModel")
    inner class MapBalance {

        @Test
        fun `formattedTotalExtras is null when totalExtras is zero`() {
            val balance = GroupPocketBalance(
                totalContributions = 500000L,
                totalExpenses = 10000L,
                virtualBalance = 470000L,
                currency = "EUR",
                totalExtras = 0L
            )

            val result = mapper.mapBalance(balance, "Trip Group")

            assertNull(result.formattedTotalExtras)
        }

        @Test
        fun `formattedTotalExtras is present when totalExtras is positive`() {
            val balance = GroupPocketBalance(
                totalContributions = 500000L,
                totalExpenses = 10000L,
                virtualBalance = 470000L,
                currency = "EUR",
                totalExtras = 125L
            )

            val result = mapper.mapBalance(balance, "Trip Group")

            assertNotNull(result.formattedTotalExtras)
            assertTrue(result.formattedTotalExtras!!.contains("1.25"))
        }

        @Test
        fun `maps all basic fields correctly`() {
            val balance = GroupPocketBalance(
                totalContributions = 500000L,
                totalExpenses = 20500L,
                virtualBalance = 451800L,
                currency = "EUR",
                totalExtras = 1200L
            )

            val result = mapper.mapBalance(balance, "My Trip")

            assertEquals("My Trip", result.groupName)
            assertEquals("EUR", result.currency)
            assertNotNull(result.formattedTotalExtras)
            // Verify formatted values contain the expected numeric portions
            assertTrue(result.formattedBalance.contains("4,518.00"))
            assertTrue(result.formattedTotalContributed.contains("5,000.00"))
            assertTrue(result.formattedTotalSpent.contains("205.00"))
            assertTrue(result.formattedTotalExtras!!.contains("12.00"))
        }

        @Test
        fun `formattedAvailableBalance is null when no scheduled holds`() {
            val balance = GroupPocketBalance(
                totalContributions = 500000L,
                totalExpenses = 10000L,
                virtualBalance = 470000L,
                currency = "EUR",
                scheduledHoldAmount = 0L
            )

            val result = mapper.mapBalance(balance, "Group")

            assertNull(result.formattedAvailableBalance)
        }

        @Test
        fun `formattedAvailableBalance is present when scheduled holds exist`() {
            val balance = GroupPocketBalance(
                totalContributions = 500000L,
                totalExpenses = 10000L,
                virtualBalance = 470000L,
                currency = "EUR",
                scheduledHoldAmount = 5000L
            )

            val result = mapper.mapBalance(balance, "Group")

            assertNotNull(result.formattedAvailableBalance)
            // Available = 470000 - 5000 = 465000 → 4,650.00
            assertTrue(result.formattedAvailableBalance!!.contains("4,650.00"))
        }
    }

    @Nested
    @DisplayName("mapMemberBalances — cashBreakdown")
    inner class MapCashBreakdown {

        private val currency = "EUR"
        private val currentUserId = "user-1"
        private val groupMemberIds = listOf("user-1", "user-2")
        private val date = LocalDateTime.of(2026, 1, 10, 9, 0)

        @Test
        fun `cashBreakdown is empty when no withdrawals provided`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 1000L)

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId
            )

            assertTrue(result[0].cashBreakdown.isEmpty())
        }

        @Test
        fun `USER-scoped withdrawal attributed fully to withdrawing user`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 100000L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date,
                title = "7-11 ATM"
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(1, breakdown.size)
            val item = breakdown[0]
            assertEquals("7-11 ATM", item.withdrawalLabel)
            assertFalse(item.isEstimatedShare)
            // EUR withdrawal → no rate or equivalent shown
            assertEquals("", item.formattedRate)
            assertEquals("", item.formattedEquivalent)
        }

        @Test
        fun `USER-scoped withdrawal not attributed to other user`() {
            val balance = MemberBalance(userId = "user-2", cashInHand = 0L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            assertTrue(result[0].cashBreakdown.isEmpty())
        }

        @Test
        fun `GROUP-scoped withdrawal split equally and marked as estimated share`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 50000L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.GROUP,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date,
                title = "Airport ATM"
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds // 2 members → equal split
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(1, breakdown.size)
            val item = breakdown[0]
            assertTrue(item.isEstimatedShare)
            assertEquals("Airport ATM", item.withdrawalLabel)
        }

        @Test
        fun `foreign currency withdrawal shows rate and equivalent`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 1342L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "THB",
                amountWithdrawn = 5000L,
                remainingAmount = 5000L,
                deductedBaseAmount = 1342L,
                exchangeRate = BigDecimal("0.027"),
                createdAt = date,
                title = "Airport ATM"
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                groupCurrency = "EUR",
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(1, breakdown.size)
            val item = breakdown[0]
            assertTrue(item.formattedRate.contains("THB/EUR"))
            assertTrue(item.formattedEquivalent.isNotBlank())
        }

        @Test
        fun `withdrawal with zero remaining is excluded from breakdown`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 0L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 0L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            assertTrue(result[0].cashBreakdown.isEmpty())
        }

        @Test
        fun `cashBreakdown is suppressed when cashInHand is negative`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = -500L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 50000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            val item = result[0]
            assertTrue(item.hasNegativeCashInHand)
            assertTrue(item.cashBreakdown.isEmpty())
        }

        @Test
        fun `blank title uses ATM-date fallback label`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 100000L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date,
                title = null
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    groupMemberIds = groupMemberIds
                )
            )

            val label = result[0].cashBreakdown[0].withdrawalLabel
            assertTrue(label.startsWith("ATM"))
        }

        @Test
        fun `SUBUNIT-scoped withdrawal attributed by memberShares`() {
            val subunit = Subunit(
                id = "sub-1",
                name = "Couple",
                memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
            )
            val balance = MemberBalance(userId = "user-1", cashInHand = 50000L)
            val withdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "sub-1",
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(withdrawal),
                    subunitsMap = mapOf("sub-1" to subunit),
                    groupMemberIds = groupMemberIds
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(1, breakdown.size)
            assertFalse(breakdown[0].isEstimatedShare)
            assertEquals("Couple", breakdown[0].scopeLabel)
        }

        @Test
        fun `GROUP items appear before USER items in breakdown order`() {
            val balance = MemberBalance(userId = "user-1", cashInHand = 60000L)
            val groupWithdrawal = CashWithdrawal(
                id = "w1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.GROUP,
                currency = "EUR",
                amountWithdrawn = 200000L,
                remainingAmount = 200000L,
                deductedBaseAmount = 200000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date.minusDays(1),
                title = "Group ATM"
            )
            val userWithdrawal = CashWithdrawal(
                id = "w2",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                currency = "EUR",
                amountWithdrawn = 100000L,
                remainingAmount = 100000L,
                deductedBaseAmount = 100000L,
                exchangeRate = BigDecimal.ONE,
                createdAt = date,
                title = "Personal ATM"
            )

            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(userWithdrawal, groupWithdrawal), // intentionally USER first
                    groupMemberIds = groupMemberIds
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(2, breakdown.size)
            // GROUP scope (scopeOrder=0) must appear before USER scope (scopeOrder=2)
            assertTrue(breakdown[0].isEstimatedShare) // GROUP
            assertFalse(breakdown[1].isEstimatedShare) // USER
        }

        @Test
        fun `breakdown order is GROUP then SUBUNIT then USER regardless of input order`() {
            val subunit = Subunit(
                id = "sub-1",
                name = "Couple",
                memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
            )
            val balance = MemberBalance(userId = "user-1", cashInHand = 150000L)

            // Input deliberately in reverse of expected order: USER → SUBUNIT → GROUP
            val result = mapper.mapMemberBalances(
                balances = listOf(balance),
                currency = currency,
                currentUserId = currentUserId,
                cashContext = MemberBalanceCashContext(
                    withdrawals = listOf(
                        cashWithdrawal("w1", PayerType.USER, createdAt = date),
                        cashWithdrawal(
                            "w2",
                            PayerType.SUBUNIT,
                            subunitId = "sub-1",
                            createdAt = date.minusDays(1),
                            amountWithdrawn = 200000L
                        ),
                        cashWithdrawal("w3", PayerType.GROUP, createdAt = date.minusDays(2), amountWithdrawn = 60000L)
                    ),
                    subunitsMap = mapOf("sub-1" to subunit),
                    groupMemberIds = groupMemberIds
                )
            )

            val breakdown = result[0].cashBreakdown
            assertEquals(3, breakdown.size)
            // Expected order: GROUP (scopeOrder=0) → SUBUNIT (scopeOrder=1) → USER (scopeOrder=2)
            assertTrue(breakdown[0].isEstimatedShare) // GROUP
            assertEquals("Couple", breakdown[1].scopeLabel) // SUBUNIT
            assertFalse(breakdown[2].isEstimatedShare) // USER
        }
    }
}

/**
 * Creates a [CashWithdrawal] test fixture with sensible defaults, keeping individual test bodies concise.
 *
 * All monetary defaults (100 000 units at 1:1 rate) are intentionally round numbers so
 * tests can assert share fractions without rounding surprises.
 */
private fun cashWithdrawal(
    id: String,
    scope: PayerType = PayerType.USER,
    withdrawnBy: String = "user-1",
    subunitId: String? = null,
    amountWithdrawn: Long = 100000L,
    currency: String = "EUR",
    createdAt: LocalDateTime? = null
) = CashWithdrawal(
    id = id,
    withdrawnBy = withdrawnBy,
    withdrawalScope = scope,
    subunitId = subunitId,
    currency = currency,
    amountWithdrawn = amountWithdrawn,
    remainingAmount = amountWithdrawn, // Defaults to full amount; no FIFO consumption
    deductedBaseAmount = amountWithdrawn,
    exchangeRate = BigDecimal.ONE,
    createdAt = createdAt
)
