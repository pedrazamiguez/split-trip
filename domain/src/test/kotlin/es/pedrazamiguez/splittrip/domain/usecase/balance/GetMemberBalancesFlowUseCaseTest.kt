package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.SettlementReconciliationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetMemberBalancesFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.strategy.ContributionAttributionStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.strategy.StandardContributionAttributionStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.MemberBalanceCalculationInputs
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetMemberBalancesFlowUseCase — Core Balance Calculations")
class GetMemberBalancesFlowUseCaseTest {
    private lateinit var useCase: GetMemberBalancesFlowUseCase
    private val groupId = "group-123"
    private val groupMemberIds = listOf("user-1", "user-2", "user-3", "user-4")

    @BeforeEach
    fun setUp() {
        useCase = GetMemberBalancesFlowUseCaseImpl(
            addOnCalculationService = AddOnCalculationServiceImpl(),
            settlementReconciliationService = SettlementReconciliationServiceImpl()
        )
    }
    private fun compute(
        contributions: List<Contribution> = emptyList(),
        withdrawals: List<CashWithdrawal> = emptyList(),
        expenses: List<Expense> = emptyList(),
        cashTransfers: List<CashTransfer> = emptyList(),
        subunits: List<Subunit> = emptyList(),
        memberIds: List<String> = groupMemberIds,
        groupCurrency: String = "EUR",
        settlements: List<SettlementRecord> = emptyList(),
        attributionStrategy: ContributionAttributionStrategy = StandardContributionAttributionStrategy
    ) = useCase.computeMemberBalances(
        MemberBalanceCalculationInputs(
            contributions = contributions,
            withdrawals = withdrawals,
            expenses = expenses,
            cashTransfers = cashTransfers,
            subunits = subunits,
            groupMemberIds = memberIds,
            groupCurrency = groupCurrency,
            settlements = settlements,
            attributionStrategy = attributionStrategy
        )
    )

    @Nested
    @DisplayName("Zero data / edge cases")
    inner class ZeroData {
        @Test
        fun `all members have zero balances when no data exists`() {
            val result = compute()
            assertEquals(4, result.size)
            result.forEach { balance ->
                assertEquals(0L, balance.contributed)
                assertEquals(0L, balance.withdrawn)
                assertEquals(0L, balance.cashSpent)
                assertEquals(0L, balance.nonCashSpent)
                assertEquals(0L, balance.totalSpent)
                assertEquals(0L, balance.pocketBalance)
                assertEquals(0L, balance.cashInHand)
            }
        }

        @Test
        fun `returns empty list when group has no members and no data`() {
            val result = compute(memberIds = emptyList())
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Contribution attribution")
    inner class ContributionAttribution {
        @Test
        fun `individual contributions attributed entirely to the contributor`() {
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 5000L),
                Contribution(userId = "user-2", contributionScope = PayerType.USER, amount = 3000L)
            )
            val result = compute(contributions = contributions)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.contributed)
            assertEquals(3000L, balanceMap["user-2"]!!.contributed)
            assertEquals(0L, balanceMap["user-3"]!!.contributed)
            assertEquals(0L, balanceMap["user-4"]!!.contributed)
        }

        @Test
        fun `GROUP-scoped contribution distributed equally among all members by default`() {
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.GROUP, amount = 10000L)
            )
            val result = compute(contributions = contributions)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(2500L, balanceMap["user-1"]!!.contributed)
            assertEquals(2500L, balanceMap["user-2"]!!.contributed)
            assertEquals(2500L, balanceMap["user-3"]!!.contributed)
            assertEquals(2500L, balanceMap["user-4"]!!.contributed)
            assertEquals(10000L, balanceMap.values.sumOf { it.contributed })
        }

        @Test
        fun `GROUP-scoped contribution remainder allocated correctly by default`() {
            // 101 / 4 = 25 each + 1 remainder unit distributed to first member
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.GROUP, amount = 101L)
            )
            val result = compute(contributions = contributions)
            val balanceMap = result.associateBy { it.userId }
            val totalContributed = balanceMap.values.sumOf { it.contributed }
            assertEquals(101L, totalContributed)
        }

        @Test
        fun `subunit contribution distributed by memberShares (50-50 couple)`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    amount = 10000L
                )
            )
            val result = compute(contributions = contributions, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.contributed)
            assertEquals(5000L, balanceMap["user-2"]!!.contributed)
        }

        @Test
        fun `subunit contribution with unequal shares (40-30-30 family)`() {
            val subunit = Subunit(
                id = "sub-fam",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.4"),
                    "user-2" to BigDecimal("0.3"),
                    "user-3" to BigDecimal("0.3")
                )
            )
            val contributions = listOf(
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-fam",
                    amount = 10000L
                )
            )
            val result = compute(contributions = contributions, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(4000L, balanceMap["user-1"]!!.contributed)
            assertEquals(3000L, balanceMap["user-2"]!!.contributed)
            assertEquals(3000L, balanceMap["user-3"]!!.contributed)
            assertEquals(10000L, balanceMap.values.sumOf { it.contributed })
        }

        @Test
        fun `mixed individual and subunit contributions`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(userId = "user-3", contributionScope = PayerType.USER, amount = 5000L),
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    amount = 10000L
                )
            )
            val result = compute(contributions = contributions, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.contributed)
            assertEquals(5000L, balanceMap["user-2"]!!.contributed)
            assertEquals(5000L, balanceMap["user-3"]!!.contributed)
        }

        @Test
        fun `subunit contribution falls back to contributor when subunit not found`() {
            val contributions = listOf(
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "nonexistent-sub",
                    amount = 8000L
                )
            )
            val result = compute(contributions = contributions)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(8000L, balanceMap["user-1"]!!.contributed)
        }

        @Test
        fun `rounding remainder allocated correctly for indivisible amounts`() {
            val thirdShare = BigDecimal.ONE.divide(BigDecimal(3), 10, RoundingMode.DOWN)
            val subunit = Subunit(
                id = "sub-3",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2", "user-3"),
                memberShares = mapOf(
                    "user-1" to thirdShare,
                    "user-2" to thirdShare,
                    "user-3" to thirdShare
                )
            )
            val contributions = listOf(
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-3",
                    amount = 100L
                )
            )
            val result = compute(contributions = contributions, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            val totalContributed = balanceMap.values.sumOf { it.contributed }
            assertEquals(100L, totalContributed)
            assertTrue(balanceMap["user-1"]!!.contributed >= 33L)
            assertTrue(balanceMap["user-2"]!!.contributed >= 33L)
            assertTrue(balanceMap["user-3"]!!.contributed >= 33L)
        }

        @Test
        fun `mixed GROUP, SUBUNIT, and USER contributions by default`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.GROUP, amount = 4000L),
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    amount = 2000L
                ),
                Contribution(userId = "user-3", contributionScope = PayerType.USER, amount = 500L)
            )
            val result = compute(contributions = contributions, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            // GROUP: 4000 / 4 = 1000 each
            // SUBUNIT: 2000 → user-1: 1000, user-2: 1000
            // USER: user-3: 500
            assertEquals(1000L + 1000L, balanceMap["user-1"]!!.contributed) // 2000
            assertEquals(1000L + 1000L, balanceMap["user-2"]!!.contributed) // 2000
            assertEquals(1000L + 500L, balanceMap["user-3"]!!.contributed) // 1500
            assertEquals(1000L, balanceMap["user-4"]!!.contributed) // 1000
        }
    }

    @Nested
    @DisplayName("Withdrawal attribution")
    inner class WithdrawalAttribution {
        @Test
        fun `GROUP-scoped withdrawal split equally among all members`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    deductedBaseAmount = 10000L
                )
            )
            val result = compute(withdrawals = withdrawals)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(2500L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(2500L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(2500L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(2500L, balanceMap["user-4"]!!.withdrawn)
        }

        @Test
        fun `GROUP-scoped withdrawal with remainder distributed correctly`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    deductedBaseAmount = 10001L
                )
            )
            val result = compute(withdrawals = withdrawals)
            val totalWithdrawn = result.sumOf { it.withdrawn }
            assertEquals(10001L, totalWithdrawn)
        }

        @Test
        fun `SUBUNIT-scoped withdrawal distributed by memberShares`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.6"),
                    "user-2" to BigDecimal("0.4")
                )
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    deductedBaseAmount = 10000L
                )
            )
            val result = compute(withdrawals = withdrawals, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(6000L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(4000L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-4"]!!.withdrawn)
        }

        @Test
        fun `USER-scoped withdrawal attributed entirely to withdrawer`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-3",
                    withdrawalScope = PayerType.USER,
                    deductedBaseAmount = 500L
                )
            )
            val result = compute(withdrawals = withdrawals)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(500L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-4"]!!.withdrawn)
        }

        @Test
        fun `mixed withdrawal scopes in one group`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    deductedBaseAmount = 20000L
                ),
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    deductedBaseAmount = 10000L
                ),
                CashWithdrawal(
                    withdrawnBy = "user-3",
                    withdrawalScope = PayerType.USER,
                    deductedBaseAmount = 500L
                )
            )
            val result = compute(withdrawals = withdrawals, subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(10000L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(10000L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(5500L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(5000L, balanceMap["user-4"]!!.withdrawn)
        }

        @Test
        fun `SUBUNIT-scoped withdrawal falls back to withdrawer when subunit not found`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "nonexistent",
                    deductedBaseAmount = 3000L
                )
            )
            val result = compute(withdrawals = withdrawals)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(3000L, balanceMap["user-1"]!!.withdrawn)
        }
    }

    @Nested
    @DisplayName("Expense split attribution")
    inner class ExpenseSplitAttribution {
        @Test
        fun `non-cash expense splits go to nonCashSpent`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    groupId = groupId,
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    splitType = SplitType.EQUAL,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2500L),
                        ExpenseSplit(userId = "user-2", amountCents = 2500L),
                        ExpenseSplit(userId = "user-3", amountCents = 2500L),
                        ExpenseSplit(userId = "user-4", amountCents = 2500L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(2500L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(2500L, balanceMap["user-2"]!!.nonCashSpent)
            assertEquals(2500L, balanceMap["user-3"]!!.nonCashSpent)
            assertEquals(2500L, balanceMap["user-4"]!!.nonCashSpent)
            // Cash spent should be zero
            result.forEach { assertEquals(0L, it.cashSpent) }
        }

        @Test
        fun `cash expense splits go to cashSpent`() {
            val expenses = listOf(
                Expense(
                    id = "exp-cash",
                    groupId = groupId,
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L),
                        ExpenseSplit(userId = "user-2", amountCents = 5000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(5000L, balanceMap["user-2"]!!.cashSpent)
            // Non-cash spent should be zero
            result.forEach { assertEquals(0L, it.nonCashSpent) }
        }

        @Test
        fun `mixed cash and non-cash expenses attributed correctly`() {
            val expenses = listOf(
                Expense(
                    id = "exp-cash",
                    sourceAmount = 5000L,
                    groupAmount = 5000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L),
                        ExpenseSplit(userId = "user-2", amountCents = 2000L)
                    )
                ),
                Expense(
                    id = "exp-card",
                    sourceAmount = 5000L,
                    groupAmount = 5000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 1500L),
                        ExpenseSplit(userId = "user-3", amountCents = 3500L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            // user-1: cashSpent=3000, nonCashSpent=1500, totalSpent=4500
            assertEquals(3000L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(1500L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(4500L, balanceMap["user-1"]!!.totalSpent)
            // user-2: cashSpent=2000, nonCashSpent=0, totalSpent=2000
            assertEquals(2000L, balanceMap["user-2"]!!.cashSpent)
            assertEquals(0L, balanceMap["user-2"]!!.nonCashSpent)
            assertEquals(2000L, balanceMap["user-2"]!!.totalSpent)
            // user-3: cashSpent=0, nonCashSpent=3500, totalSpent=3500
            assertEquals(0L, balanceMap["user-3"]!!.cashSpent)
            assertEquals(3500L, balanceMap["user-3"]!!.nonCashSpent)
            assertEquals(3500L, balanceMap["user-3"]!!.totalSpent)
            // user-4: nothing
            assertEquals(0L, balanceMap["user-4"]!!.totalSpent)
        }

        @Test
        fun `default payment method OTHER routes to nonCashSpent`() {
            // Expense without explicit paymentMethod → defaults to OTHER (non-cash)
            val expenses = listOf(
                Expense(
                    id = "exp-default",
                    sourceAmount = 6000L,
                    groupAmount = 6000L,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 6000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(0L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(6000L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(6000L, balanceMap["user-1"]!!.totalSpent)
        }

        @Test
        fun `excluded splits are not counted`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L),
                        ExpenseSplit(userId = "user-2", amountCents = 5000L, isExcluded = true)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(0L, balanceMap["user-2"]!!.cashSpent)
        }

        @Test
        fun `foreign currency expense splits converted to group currency`() {
            val expenses = listOf(
                Expense(
                    id = "exp-thb",
                    sourceAmount = 100000L,
                    groupAmount = 2683L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 50000L),
                        ExpenseSplit(userId = "user-2", amountCents = 50000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(1342L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(1342L, balanceMap["user-2"]!!.cashSpent)
            val totalCashSpent = result.sumOf { it.cashSpent }
            assertTrue(totalCashSpent in 2683L..2684L)
        }

        @Test
        fun `mixed same-currency and foreign-currency expenses with different payment methods`() {
            val expenses = listOf(
                Expense(
                    id = "exp-eur",
                    sourceAmount = 4000L,
                    groupAmount = 4000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2000L),
                        ExpenseSplit(userId = "user-2", amountCents = 2000L)
                    )
                ),
                Expense(
                    id = "exp-thb",
                    sourceAmount = 100000L,
                    groupAmount = 2700L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 50000L),
                        ExpenseSplit(userId = "user-2", amountCents = 50000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            // user-1: nonCashSpent=2000 (EUR card), cashSpent=1350 (THB cash), totalSpent=3350
            assertEquals(1350L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(2000L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(3350L, balanceMap["user-1"]!!.totalSpent)
            // user-2: same
            assertEquals(1350L, balanceMap["user-2"]!!.cashSpent)
            assertEquals(2000L, balanceMap["user-2"]!!.nonCashSpent)
            assertEquals(3350L, balanceMap["user-2"]!!.totalSpent)
        }

        @Test
        fun `zero sourceAmount expense produces zero spent`() {
            val expenses = listOf(
                Expense(
                    id = "exp-zero",
                    sourceAmount = 0L,
                    groupAmount = 5000L,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2500L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            assertEquals(0L, balanceMap["user-1"]!!.totalSpent)
        }
    }

    @Nested
    @DisplayName("Pocket balance and cash in hand calculation")
    inner class BalanceCalculation {
        @Test
        fun `pocketBalance equals contributed minus withdrawn minus nonCashSpent`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(
                    userId = "user-1",
                    contributionScope = PayerType.SUBUNIT,
                    subunitId = "sub-1",
                    amount = 10000L
                ),
                Contribution(userId = "user-3", contributionScope = PayerType.USER, amount = 5000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    deductedBaseAmount = 4000L
                )
            )
            // Non-cash expense: card payment
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 8000L,
                    groupAmount = 8000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2000L),
                        ExpenseSplit(userId = "user-2", amountCents = 2000L),
                        ExpenseSplit(userId = "user-3", amountCents = 2000L),
                        ExpenseSplit(userId = "user-4", amountCents = 2000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                subunits = listOf(subunit)
            )
            val balanceMap = result.associateBy { it.userId }
            // pocketBalance = contributed - withdrawn - nonCashSpent
            // user-1: 5000 - 1000 - 2000 = 2000
            assertEquals(2000L, balanceMap["user-1"]!!.pocketBalance)
            // user-2: 5000 - 1000 - 2000 = 2000
            assertEquals(2000L, balanceMap["user-2"]!!.pocketBalance)
            // user-3: 5000 - 1000 - 2000 = 2000
            assertEquals(2000L, balanceMap["user-3"]!!.pocketBalance)
            // user-4: 0 - 1000 - 2000 = -3000
            assertEquals(-3000L, balanceMap["user-4"]!!.pocketBalance)
            // Sum of pocketBalances = totalContributed - totalWithdrawn - totalNonCashSpent
            val totalPocket = result.sumOf { it.pocketBalance }
            assertEquals(15000L - 4000L - 8000L, totalPocket)
        }

        @Test
        fun `cashInHand equals sum of remaining amounts attributed by scope`() {
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 10000L)
            )
            // remainingAmount = 2000 (5000 withdrawn, 3000 consumed by the cash expense below)
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 5000L,
                    remainingAmount = 2000L,
                    deductedBaseAmount = 5000L,
                    currency = "EUR"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 3000L,
                    groupAmount = 3000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses
            )
            val balanceMap = result.associateBy { it.userId }
            assertEquals(5000L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(3000L, balanceMap["user-1"]!!.cashSpent)
            // cashInHand = remainingAmount × (deductedBaseAmount / amountWithdrawn) = 2000 × 1.0 = 2000
            assertEquals(2000L, balanceMap["user-1"]!!.cashInHand)
            // pocketBalance = contributed - withdrawn - nonCashSpent = 10000 - 5000 - 0 = 5000
            assertEquals(5000L, balanceMap["user-1"]!!.pocketBalance)
        }

        @Test
        fun `negative pocketBalance indicates member overdrew from pocket`() {
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 1000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    deductedBaseAmount = 5000L
                )
            )
            val result = compute(contributions = contributions, withdrawals = withdrawals)
            val balanceMap = result.associateBy { it.userId }
            // pocketBalance = 1000 - 5000 - 0 = -4000
            assertEquals(-4000L, balanceMap["user-1"]!!.pocketBalance)
        }

        @Test
        fun `member with only non-cash expenses has zero cashInHand`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 5000L,
                    groupAmount = 5000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            // pocketBalance = 0 - 0 - 5000 = -5000
            assertEquals(-5000L, balanceMap["user-1"]!!.pocketBalance)
            assertEquals(5000L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(0L, balanceMap["user-1"]!!.cashInHand)
        }

        @Test
        fun `cash expense does not reduce pocketBalance`() {
            // Cash expense is funded from physical cash (withdrawals),
            // not from the virtual pocket — only nonCashSpent reduces pocket.
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 10000L)
            )
            // remainingAmount = 2000: 5000 withdrawn, 3000 consumed by cash expense below
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 5000L,
                    remainingAmount = 2000L,
                    deductedBaseAmount = 5000L,
                    currency = "EUR"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-cash",
                    sourceAmount = 3000L,
                    groupAmount = 3000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses
            )
            val balanceMap = result.associateBy { it.userId }
            // pocketBalance = 10000 - 5000 - 0 = 5000 (cash expense does NOT reduce pocket)
            assertEquals(5000L, balanceMap["user-1"]!!.pocketBalance)
            // cashInHand = remainingAmount × rate = 2000 × 1.0 = 2000
            assertEquals(2000L, balanceMap["user-1"]!!.cashInHand)
        }

        @Test
        fun `non-cash expense reduces pocketBalance but not cashInHand`() {
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 10000L)
            )
            // remainingAmount = 5000: no cash expense, so nothing consumed
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 5000L,
                    remainingAmount = 5000L,
                    deductedBaseAmount = 5000L,
                    currency = "EUR"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-card",
                    sourceAmount = 3000L,
                    groupAmount = 3000L,
                    paymentMethod = PaymentMethod.BIZUM,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses
            )
            val balanceMap = result.associateBy { it.userId }
            // pocketBalance = 10000 - 5000 - 3000 = 2000 (non-cash reduces pocket)
            assertEquals(2000L, balanceMap["user-1"]!!.pocketBalance)
            // cashInHand = remainingAmount × rate = 5000 × 1.0 = 5000 (non-cash does NOT reduce physical cash)
            assertEquals(5000L, balanceMap["user-1"]!!.cashInHand)
        }
    }

    @Nested
    @DisplayName("cashInHand: sum-of-remaining-amounts approach")
    inner class CashInHandRemainingAmount {

        @Test
        fun `USER-scoped withdrawal cashInHand equals remainingAmount when same currency`() {
            // 8000 EUR withdrawn, 3000 EUR consumed → 5000 EUR remaining
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 8000L,
                remainingAmount = 5000L,
                deductedBaseAmount = 8000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(withdrawal))
            val balanceMap = result.associateBy { it.userId }
            // groupCurrencyRemaining = 5000 × 8000/8000 = 5000, all to user-1 (USER scope)
            assertEquals(5000L, balanceMap["user-1"]!!.cashInHand)
            assertEquals(0L, balanceMap["user-2"]!!.cashInHand)
        }

        @Test
        fun `GROUP-scoped withdrawal cashInHand split equally from remainingAmount`() {
            // 10000 EUR withdrawn, 4000 consumed → 6000 remaining, split 4 ways
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.GROUP,
                amountWithdrawn = 10000L,
                remainingAmount = 6000L,
                deductedBaseAmount = 10000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(withdrawal))
            val balanceMap = result.associateBy { it.userId }
            // 6000 / 4 = 1500 each
            assertEquals(1500L, balanceMap["user-1"]!!.cashInHand)
            assertEquals(1500L, balanceMap["user-2"]!!.cashInHand)
            assertEquals(1500L, balanceMap["user-3"]!!.cashInHand)
            assertEquals(1500L, balanceMap["user-4"]!!.cashInHand)
            assertEquals(6000L, result.sumOf { it.cashInHand })
        }

        @Test
        fun `SUBUNIT-scoped withdrawal cashInHand distributed by memberShares`() {
            val subunit = Subunit(
                id = "sub-1",
                groupId = groupId,
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.6"),
                    "user-2" to BigDecimal("0.4")
                )
            )
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "sub-1",
                amountWithdrawn = 10000L,
                remainingAmount = 8000L,
                deductedBaseAmount = 10000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(withdrawal), subunits = listOf(subunit))
            val balanceMap = result.associateBy { it.userId }
            // user-1: 8000 × 0.6 = 4800, user-2: 8000 × 0.4 = 3200
            assertEquals(4800L, balanceMap["user-1"]!!.cashInHand)
            assertEquals(3200L, balanceMap["user-2"]!!.cashInHand)
            assertEquals(0L, balanceMap["user-3"]!!.cashInHand)
            assertEquals(8000L, result.sumOf { it.cashInHand })
        }

        @Test
        fun `cross-currency withdrawal cashInHand converted using ATM rate`() {
            // 10,000 THB withdrawal (in cents: 1,000,000) at rate ~37
            // deductedBaseAmount = 27,000 EUR-cents (270 EUR)
            // 5,000 THB remaining (in cents: 500,000)
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 1000000L,
                remainingAmount = 500000L,
                deductedBaseAmount = 27000L,
                currency = "THB"
            )
            val result = compute(withdrawals = listOf(withdrawal))
            val balanceMap = result.associateBy { it.userId }
            // groupCurrencyRemaining = 500000 × 27000 / 1000000 = 13500 EUR-cents
            assertEquals(13500L, balanceMap["user-1"]!!.cashInHand)
        }

        @Test
        fun `withdrawal with amountWithdrawn zero is skipped to avoid division by zero`() {
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 0L,
                remainingAmount = 5000L,
                deductedBaseAmount = 5000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(withdrawal))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(0L, balanceMap["user-1"]!!.cashInHand)
        }

        @Test
        fun `multiple USER withdrawals for same user accumulate cashInHand`() {
            val w1 = CashWithdrawal(
                id = "w-1",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 5000L,
                remainingAmount = 3000L,
                deductedBaseAmount = 5000L,
                currency = "EUR"
            )
            val w2 = CashWithdrawal(
                id = "w-2",
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 5000L,
                remainingAmount = 5000L,
                deductedBaseAmount = 5000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(w1, w2))
            val balanceMap = result.associateBy { it.userId }
            // 3000 + 5000 = 8000
            assertEquals(8000L, balanceMap["user-1"]!!.cashInHand)
        }

        @Test
        fun `fully exhausted withdrawal contributes zero cashInHand`() {
            val withdrawal = CashWithdrawal(
                withdrawnBy = "user-1",
                withdrawalScope = PayerType.USER,
                amountWithdrawn = 5000L,
                remainingAmount = 0L,
                deductedBaseAmount = 5000L,
                currency = "EUR"
            )
            val result = compute(withdrawals = listOf(withdrawal))
            val balanceMap = result.associateBy { it.userId }
            assertEquals(0L, balanceMap["user-1"]!!.cashInHand)
        }
    }

    @Nested
    @DisplayName("RefundableExpenses")
    inner class RefundableExpenses {

        @Test
        fun `refundable expenses populate refundable fields`() {
            val expenses = listOf(
                Expense(
                    id = "exp-ref",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.DEBIT_CARD,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            assertEquals(10000L, balance.refundableSpent)
            assertEquals(1, balance.refundableSpentByCurrency.size)
            assertEquals("EUR", balance.refundableSpentByCurrency[0].currency)
            assertEquals(10000L, balance.refundableSpentByCurrency[0].amountCents)
            assertEquals(10000L, balance.nonCashSpent)
            assertEquals(-10000L, balance.pocketBalance)
        }

        @Test
        fun `refundable expenses are included in totalSpent and reduce pocketBalance`() {
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 20000L)
            )
            val expenses = listOf(
                Expense(
                    id = "exp-ref",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.DEBIT_CARD,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L)
                    )
                )
            )
            val result = compute(contributions = contributions, expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            // totalSpent includes refundable non-cash spent: cash (0) + non-cash (10000) = 10000
            assertEquals(10000L, balance.totalSpent)
            // pocketBalance is reduced by refundable: 20000 - 0 (withdrawn) - 10000 (nonCashSpent) = 10000
            assertEquals(10000L, balance.pocketBalance)
        }

        @Test
        fun `refundable expense with future dueDate populates refundable fields`() {
            val expenses = listOf(
                Expense(
                    id = "exp-ref-future",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.DEBIT_CARD,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    dueDate = LocalDateTime.now().plusDays(3),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            assertEquals(10000L, balance.refundableSpent)
            assertEquals(1, balance.refundableSpentByCurrency.size)
            assertEquals("EUR", balance.refundableSpentByCurrency[0].currency)
            assertEquals(10000L, balance.refundableSpentByCurrency[0].amountCents)
            assertEquals(10000L, balance.nonCashSpent)
            assertEquals(-10000L, balance.pocketBalance)
        }

        @Test
        fun `refundable expense with today dueDate populates refundable fields`() {
            val expenses = listOf(
                Expense(
                    id = "exp-ref-today",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.DEBIT_CARD,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    dueDate = LocalDateTime.now(),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L)
                    )
                )
            )
            val result = compute(expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            assertEquals(10000L, balance.refundableSpent)
            assertEquals(1, balance.refundableSpentByCurrency.size)
            assertEquals("EUR", balance.refundableSpentByCurrency[0].currency)
            assertEquals(10000L, balance.refundableSpentByCurrency[0].amountCents)
        }

        @Test
        fun `expired non-cash refundable expense is excluded from refundableSpent but settles normally`() {
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 20000L)
            )
            val expenses = listOf(
                Expense(
                    id = "exp-ref-expired",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.DEBIT_CARD,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    dueDate = LocalDateTime.now().minusDays(2),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L)
                    )
                )
            )
            val result = compute(contributions = contributions, expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            assertEquals(0L, balance.refundableSpent)
            assertTrue(balance.refundableSpentByCurrency.isEmpty())
            assertEquals(10000L, balance.nonCashSpent)
            assertEquals(10000L, balance.totalSpent)
            assertEquals(10000L, balance.pocketBalance)
        }

        @Test
        fun `expired cash refundable expense is excluded from refundableSpent but settles cash spent`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    id = "wd-1",
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 10000L,
                    remainingAmount = 5000L,
                    deductedBaseAmount = 10000L
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-cash-ref",
                    groupId = groupId,
                    payerId = "user-1",
                    payerType = PayerType.USER,
                    paymentMethod = PaymentMethod.CASH,
                    paymentStatus = PaymentStatus.REFUNDABLE,
                    dueDate = LocalDateTime.now().minusDays(1),
                    sourceAmount = 5000L,
                    sourceCurrency = "EUR",
                    groupAmount = 5000L,
                    groupCurrency = "EUR",
                    cashTranches = listOf(CashTranche("wd-1", 5000L)),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L)
                    )
                )
            )
            val result = compute(withdrawals = withdrawals, expenses = expenses)
            val balanceMap = result.associateBy { it.userId }
            val balance = balanceMap["user-1"]!!

            assertEquals(0L, balance.refundableSpent)
            assertTrue(balance.refundableSpentByCurrency.isEmpty())
            assertEquals(5000L, balance.cashSpent)
            assertEquals(5000L, balance.totalSpent)
            assertEquals(5000L, balance.cashInHand)
        }
    }

    @Nested
    @DisplayName("Settlement reconciliation")
    inner class SettlementReconciliation {
        @Test
        fun `resolved pocket settlement adjusts contributed, withdrawn, and pocketBalance`() {
            // Andres (user-1) contributed 10000, Antonio (user-2) withdrew 10000.
            // Antonio has -10000 pocket balance. Andres has +10000 pocket balance.
            // Resolved pocket settlement of 10000 from user-2 to user-1.
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 10000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-2",
                    withdrawalScope = PayerType.USER,
                    deductedBaseAmount = 10000L
                )
            )
            val settlements = listOf(
                SettlementRecord(
                    id = "settlement-1",
                    groupId = groupId,
                    settlement = Settlement(
                        fromUserId = "user-2",
                        toUserId = "user-1",
                        amount = 10000L,
                        currency = "EUR",
                        sourcePocket = SettlementPocketType.POCKET
                    ),
                    status = SettlementStatus.RESOLVED,
                    createdAt = LocalDateTime.now()
                )
            )

            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                settlements = settlements
            )
            val balanceMap = result.associateBy { it.userId }

            // Payer user-2: contributed increases by 10000, pocketBalance increases by 10000 (from -10000 to 0)
            assertEquals(10000L, balanceMap["user-2"]!!.contributed)
            assertEquals(0L, balanceMap["user-2"]!!.pocketBalance)

            // Creditor user-1: withdrawn increases by 10000, pocketBalance decreases by 10000 (from 10000 to 0)
            assertEquals(10000L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-1"]!!.pocketBalance)
        }

        @Test
        fun `resolved cash settlement adjusts withdrawn and contributed without altering cashInHand`() {
            // user-1 withdrew 10000 cash (has 10000 cash in hand).
            // user-2 spent 6000 cash.
            // Resolved cash settlement of 6000 cash from user-2 to user-1.
            val withdrawals = listOf(
                CashWithdrawal(
                    id = "wd-1",
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 10000L,
                    remainingAmount = 4000L, // 6000 was consumed by the expense
                    deductedBaseAmount = 10000L
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    groupId = groupId,
                    payerId = "user-2",
                    payerType = PayerType.USER,
                    paymentMethod = PaymentMethod.CASH,
                    sourceAmount = 6000L,
                    sourceCurrency = "EUR",
                    groupAmount = 6000L,
                    groupCurrency = "EUR",
                    expectedGroupAmount = 6000L,
                    cashTranches = listOf(CashTranche("wd-1", 6000L))
                )
            )
            val cashTransfers = listOf(
                CashTransfer(
                    id = "cash-transfer-1",
                    groupId = groupId,
                    fromUserId = "user-2", // user-2 pays user-1
                    toUserId = "user-1",
                    amountCents = 6000L,
                    currency = "EUR",
                    equivalentBaseAmountCents = 6000L,
                    createdAt = System.currentTimeMillis()
                )
            )

            val result = compute(withdrawals = withdrawals, expenses = expenses, cashTransfers = cashTransfers)
            val balanceMap = result.associateBy { it.userId }

            // Payer user-2: spent 0 (no splits in expense). Pays 6000 cash to user-1.
            // withdrawn INCREASES by 6000 to offset cash debt.
            // pocketBalance is UNCHANGED (cash settlements don't affect virtual pocket).
            // cashInHand DECREASES by 6000 (gave physical cash away).
            assertEquals(0L, balanceMap["user-2"]!!.cashSpent)
            assertEquals(6000L, balanceMap["user-2"]!!.withdrawn) // Increased by 6000
            assertEquals(0L, balanceMap["user-2"]!!.pocketBalance) // Unchanged (no pocket impact)
            assertEquals(0L, balanceMap["user-2"]!!.contributed)
            assertEquals(-6000L, balanceMap["user-2"]!!.cashInHand)

            // Creditor user-1: withdrew 10000. Receives 6000 cash from user-2.
            // withdrawn DECREASES by 6000 to offset cash surplus.
            // pocketBalance is UNCHANGED (cash settlements don't affect virtual pocket).
            // cashInHand INCREASES by 6000 (received physical cash).
            assertEquals(4000L, balanceMap["user-1"]!!.withdrawn) // 10000 - 6000
            assertEquals(0L, balanceMap["user-1"]!!.contributed)
            assertEquals(10000L, balanceMap["user-1"]!!.cashInHand) // 4000 + 6000
        }
    }
}
