package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetMemberBalancesFlowUseCaseImpl
import java.math.BigDecimal
import java.math.RoundingMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetMemberBalancesFlowUseCase — Full Scenarios & Distribution Helpers")
class GetMemberBalancesFlowUseCaseScenarioTest {

    private lateinit var useCase: GetMemberBalancesFlowUseCase
    private val groupId = "group-123"
    private val groupMemberIds = listOf("user-1", "user-2", "user-3", "user-4")

    @BeforeEach
    fun setUp() {
        useCase = GetMemberBalancesFlowUseCaseImpl(AddOnCalculationServiceImpl())
    }

    private fun compute(
        contributions: List<Contribution> = emptyList(),
        withdrawals: List<CashWithdrawal> = emptyList(),
        expenses: List<Expense> = emptyList(),
        subunits: List<Subunit> = emptyList(),
        memberIds: List<String> = groupMemberIds,
        groupCurrency: String = "EUR"
    ) = useCase.computeMemberBalances(contributions, withdrawals, expenses, subunits, memberIds, groupCurrency)

    @Nested
    @DisplayName("Full scenario — wiki example")
    inner class FullScenario {
        @Test
        @Suppress("LongMethod") // Integration test scenario — length is test data + assertions, not logic
        fun `solo users plus couples plus families in one group`() {
            val couple = Subunit(
                id = "sub-couple",
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
                    subunitId = "sub-couple",
                    amount = 10000L
                ),
                Contribution(userId = "user-3", contributionScope = PayerType.USER, amount = 5000L),
                Contribution(userId = "user-4", contributionScope = PayerType.USER, amount = 5000L)
            )
            val withdrawals = listOf(
                // GROUP: 20000 EUR withdrawn. After dinner FIFO (12000 consumed), remaining = 8000.
                CashWithdrawal(
                    withdrawnBy = "user-3",
                    withdrawalScope = PayerType.GROUP,
                    amountWithdrawn = 20000L,
                    remainingAmount = 8000L, // post-FIFO: 20000 - 12000 (dinner) = 8000
                    currency = "EUR",
                    deductedBaseAmount = 20000L
                ),
                // SUBUNIT (couple): 5000 EUR withdrawn, not consumed by any cash expense.
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "sub-couple",
                    amountWithdrawn = 5000L,
                    remainingAmount = 5000L,
                    currency = "EUR",
                    deductedBaseAmount = 5000L
                ),
                // USER user-3: 500 EUR withdrawn, not consumed by any cash expense.
                CashWithdrawal(
                    withdrawnBy = "user-3",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 500L,
                    remainingAmount = 500L,
                    currency = "EUR",
                    deductedBaseAmount = 500L
                )
            )
            // Cash expense
            val expenses = listOf(
                Expense(
                    id = "exp-dinner",
                    sourceAmount = 12000L,
                    groupAmount = 12000L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L),
                        ExpenseSplit(userId = "user-2", amountCents = 3000L),
                        ExpenseSplit(userId = "user-3", amountCents = 3000L),
                        ExpenseSplit(userId = "user-4", amountCents = 3000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                subunits = listOf(couple)
            )
            val balanceMap = result.associateBy { it.userId }

            // user-1: contributed=5000, withdrawn=7500, cashSpent=3000
            assertEquals(5000L, balanceMap["user-1"]!!.contributed)
            assertEquals(7500L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(3000L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(0L, balanceMap["user-1"]!!.nonCashSpent)
            // cashInHand: GROUP(8000/4=2000) + SUBUNIT(5000/2=2500) = 4500
            assertEquals(4500L, balanceMap["user-1"]!!.cashInHand)
            // pocketBalance = 5000 - 7500 - 0 = -2500
            assertEquals(5000L - 7500L, balanceMap["user-1"]!!.pocketBalance)

            // user-2: contributed=5000, withdrawn=7500, cashSpent=3000
            assertEquals(5000L, balanceMap["user-2"]!!.contributed)
            assertEquals(7500L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(3000L, balanceMap["user-2"]!!.cashSpent)
            // cashInHand: GROUP(8000/4=2000) + SUBUNIT(5000/2=2500) = 4500
            assertEquals(4500L, balanceMap["user-2"]!!.cashInHand)
            assertEquals(-2500L, balanceMap["user-2"]!!.pocketBalance)

            // user-3: contributed=5000, withdrawn=5500, cashSpent=3000
            assertEquals(5000L, balanceMap["user-3"]!!.contributed)
            assertEquals(5500L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(3000L, balanceMap["user-3"]!!.cashSpent)
            // cashInHand: GROUP(8000/4=2000) + USER(500) = 2500
            assertEquals(2500L, balanceMap["user-3"]!!.cashInHand)
            assertEquals(-500L, balanceMap["user-3"]!!.pocketBalance)

            // user-4: contributed=5000, withdrawn=5000, cashSpent=3000
            assertEquals(5000L, balanceMap["user-4"]!!.contributed)
            assertEquals(5000L, balanceMap["user-4"]!!.withdrawn)
            assertEquals(3000L, balanceMap["user-4"]!!.cashSpent)
            // cashInHand: GROUP(8000/4=2000)
            assertEquals(2000L, balanceMap["user-4"]!!.cashInHand)
            assertEquals(0L, balanceMap["user-4"]!!.pocketBalance)

            // Invariant: Σ pocketBalance = totalContributed - totalWithdrawn - totalNonCashSpent
            val totalContributed = result.sumOf { it.contributed }
            val totalWithdrawn = result.sumOf { it.withdrawn }
            val totalNonCashSpent = result.sumOf { it.nonCashSpent }
            assertEquals(totalContributed - totalWithdrawn - totalNonCashSpent, result.sumOf { it.pocketBalance })
            // Invariant: Σ cashInHand = totalWithdrawn - totalCashSpent
            val totalCashSpent = result.sumOf { it.cashSpent }
            assertEquals(totalWithdrawn - totalCashSpent, result.sumOf { it.cashInHand })
        }

        @Test
        fun `mixed cash and non-cash expenses in multi-currency trip`() {
            val twoMembers = listOf("user-1", "user-2")
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 10000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    deductedBaseAmount = 2700L
                )
            )
            val expenses = listOf(
                // Non-cash EUR dinner (card)
                Expense(
                    id = "exp-dinner-eur",
                    sourceAmount = 4000L,
                    groupAmount = 4000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2000L),
                        ExpenseSplit(userId = "user-2", amountCents = 2000L)
                    )
                ),
                // Cash THB taxi
                Expense(
                    id = "exp-taxi-thb",
                    sourceAmount = 100000L,
                    groupAmount = 2700L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 50000L),
                        ExpenseSplit(userId = "user-2", amountCents = 50000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = twoMembers
            )
            val balanceMap = result.associateBy { it.userId }

            // user-1: contributed=10000, withdrawn=1350, nonCashSpent=2000, cashSpent=1350
            assertEquals(10000L, balanceMap["user-1"]!!.contributed)
            assertEquals(1350L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(2000L, balanceMap["user-1"]!!.nonCashSpent)
            assertEquals(1350L, balanceMap["user-1"]!!.cashSpent)
            assertEquals(3350L, balanceMap["user-1"]!!.totalSpent)
            // pocketBalance = 10000 - 1350 - 2000 = 6650
            assertEquals(6650L, balanceMap["user-1"]!!.pocketBalance)
            // cashInHand = 1350 - 1350 = 0
            assertEquals(0L, balanceMap["user-1"]!!.cashInHand)

            // user-2: contributed=0, withdrawn=1350, nonCashSpent=2000, cashSpent=1350
            assertEquals(0L, balanceMap["user-2"]!!.contributed)
            assertEquals(1350L, balanceMap["user-2"]!!.withdrawn)
            assertEquals(2000L, balanceMap["user-2"]!!.nonCashSpent)
            assertEquals(1350L, balanceMap["user-2"]!!.cashSpent)
            assertEquals(3350L, balanceMap["user-2"]!!.totalSpent)
            // pocketBalance = 0 - 1350 - 2000 = -3350
            assertEquals(-3350L, balanceMap["user-2"]!!.pocketBalance)
            // cashInHand = 1350 - 1350 = 0
            assertEquals(0L, balanceMap["user-2"]!!.cashInHand)
        }

        @Test
        fun `issue 612 scenario - cash expense does not affect pocket, non-cash does`() {
            // This test verifies the exact scenario from issue #612:
            // A member has 20€ withdrawn and pays 15€ credit card dinner.
            // Current (WRONG): available = 20 - 15 = 5€
            // Correct: cashInHand = 20 - 0 = 20€, pocketBalance = contributed - 20 - 15
            val twoMembers = listOf("user-1", "user-2")
            val contributions = listOf(
                Contribution(userId = "user-1", amount = 5000L), // 50€
                Contribution(userId = "user-2", amount = 5000L) // 50€
            )
            val withdrawals = listOf(
                // 20€ USER cash withdrawal; non-cash expense does NOT consume via FIFO
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 2000L, // 20€
                    remainingAmount = 2000L, // unchanged: credit-card expense doesn't trigger FIFO
                    deductedBaseAmount = 2000L // 20€
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-dinner",
                    sourceAmount = 1500L,
                    groupAmount = 1500L,
                    paymentMethod = PaymentMethod.CREDIT_CARD, // Non-cash!
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 1500L) // 15€
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = twoMembers
            )
            val balanceMap = result.associateBy { it.userId }

            // user-1: cashInHand = remainingAmount × (deductedBaseAmount/amountWithdrawn) = 2000 × 1.0 = 2000 cents (20€)
            assertEquals(2000L, balanceMap["user-1"]!!.cashInHand)
            // user-1: pocketBalance = 50 - 20 - 15 = 15€
            assertEquals(1500L, balanceMap["user-1"]!!.pocketBalance)
            // user-1: nonCashSpent = 15€
            assertEquals(1500L, balanceMap["user-1"]!!.nonCashSpent)
            // user-1: cashSpent = 0€
            assertEquals(0L, balanceMap["user-1"]!!.cashSpent)
        }
    }

    @Nested
    @DisplayName("distributeByShares — static helper")
    inner class DistributeByShares {
        @Test
        fun `empty shares returns empty map`() {
            val result = GetMemberBalancesFlowUseCase.distributeByShares(10000L, emptyMap())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `single member gets full amount`() {
            val result = GetMemberBalancesFlowUseCase.distributeByShares(
                10000L,
                mapOf("user-1" to BigDecimal.ONE)
            )
            assertEquals(10000L, result["user-1"])
        }

        @Test
        fun `two members 50-50`() {
            val result = GetMemberBalancesFlowUseCase.distributeByShares(
                10000L,
                mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5"))
            )
            assertEquals(5000L, result["user-1"])
            assertEquals(5000L, result["user-2"])
        }

        @Test
        fun `total distributed equals original amount despite rounding`() {
            val thirdShare = BigDecimal.ONE.divide(BigDecimal(3), 10, RoundingMode.DOWN)
            val result = GetMemberBalancesFlowUseCase.distributeByShares(
                10000L,
                mapOf(
                    "user-1" to thirdShare,
                    "user-2" to thirdShare,
                    "user-3" to thirdShare
                )
            )
            assertEquals(10000L, result.values.sum())
        }

        @Test
        fun `remainder allocation is deterministic regardless of map insertion order`() {
            val thirdShare = BigDecimal.ONE.divide(BigDecimal(3), 10, RoundingMode.DOWN)
            val mapABC = linkedMapOf(
                "alice" to thirdShare,
                "bob" to thirdShare,
                "charlie" to thirdShare
            )
            val mapCBA = linkedMapOf(
                "charlie" to thirdShare,
                "bob" to thirdShare,
                "alice" to thirdShare
            )
            val resultABC = GetMemberBalancesFlowUseCase.distributeByShares(100L, mapABC)
            val resultCBA = GetMemberBalancesFlowUseCase.distributeByShares(100L, mapCBA)
            assertEquals(resultABC, resultCBA)
            assertEquals(100L, resultABC.values.sum())
        }
    }

    @Nested
    @DisplayName("distributeEvenly — static helper")
    inner class DistributeEvenly {
        @Test
        fun `empty members returns empty map`() {
            val result = GetMemberBalancesFlowUseCase.distributeEvenly(10000L, emptyList())
            assertTrue(result.isEmpty())
        }

        @Test
        fun `evenly divisible amount`() {
            val result = GetMemberBalancesFlowUseCase.distributeEvenly(
                10000L,
                listOf("user-1", "user-2", "user-3", "user-4")
            )
            assertEquals(2500L, result["user-1"])
            assertEquals(2500L, result["user-2"])
            assertEquals(2500L, result["user-3"])
            assertEquals(2500L, result["user-4"])
        }

        @Test
        fun `total distributed equals original amount with remainder`() {
            val result = GetMemberBalancesFlowUseCase.distributeEvenly(
                10001L,
                listOf("user-1", "user-2", "user-3", "user-4")
            )
            assertEquals(10001L, result.values.sum())
        }

        @Test
        fun `remainder allocation is deterministic regardless of input order`() {
            val resultAsc = GetMemberBalancesFlowUseCase.distributeEvenly(
                10001L,
                listOf("user-1", "user-2", "user-3")
            )
            val resultDesc = GetMemberBalancesFlowUseCase.distributeEvenly(
                10001L,
                listOf("user-3", "user-1", "user-2")
            )
            assertEquals(resultAsc, resultDesc)
            // "user-1" gets the extra cent (sorted first)
            assertEquals(3334L, resultAsc["user-1"])
            assertEquals(3334L, resultDesc["user-1"])
        }
    }

    // ── Out-of-pocket scenarios ──────────────────────────────────────────────

    @Nested
    @DisplayName("Out-of-pocket expense scenarios")
    inner class OutOfPocketScenarios {

        private val twoMembers = listOf("maria", "andres")

        @Test
        fun `4-member dinner - payer gets credit, others get debt`() {
            // María pays €165 out-of-pocket dinner, split equally among 4 members.
            // Paired contribution: 16500 (effective = groupAmount, no add-ons)
            // Each member's split = 4125 (16500 / 4)
            val fourMembers = listOf("maria", "user-2", "user-3", "user-4")
            val contributions = listOf(
                Contribution(
                    userId = "maria",
                    amount = 16500L,
                    contributionScope = PayerType.USER,
                    linkedExpenseId = "exp-dinner"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-dinner",
                    sourceAmount = 16500L,
                    groupAmount = 16500L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    payerType = PayerType.USER,
                    payerId = "maria",
                    splits = listOf(
                        ExpenseSplit(userId = "maria", amountCents = 4125L),
                        ExpenseSplit(userId = "user-2", amountCents = 4125L),
                        ExpenseSplit(userId = "user-3", amountCents = 4125L),
                        ExpenseSplit(userId = "user-4", amountCents = 4125L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                expenses = expenses,
                memberIds = fourMembers
            )
            val balanceMap = result.associateBy { it.userId }

            // María: contributed=16500, nonCashSpent=4125
            // pocketBalance = 16500 - 0 - 4125 = 12375
            assertEquals(16500L, balanceMap["maria"]!!.contributed)
            assertEquals(4125L, balanceMap["maria"]!!.nonCashSpent)
            assertEquals(12375L, balanceMap["maria"]!!.pocketBalance)

            // Others: contributed=0, nonCashSpent=4125
            // pocketBalance = 0 - 0 - 4125 = -4125
            assertEquals(-4125L, balanceMap["user-2"]!!.pocketBalance)
            assertEquals(-4125L, balanceMap["user-3"]!!.pocketBalance)
            assertEquals(-4125L, balanceMap["user-4"]!!.pocketBalance)
        }

        @Test
        fun `2-member split - payer at positive, other at negative`() {
            // Andrés pays €300 for tickets, split equally between Andrés and Antonio
            val contributions = listOf(
                Contribution(
                    userId = "andres",
                    amount = 30000L,
                    contributionScope = PayerType.USER,
                    linkedExpenseId = "exp-tickets"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-tickets",
                    sourceAmount = 30000L,
                    groupAmount = 30000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    payerType = PayerType.USER,
                    payerId = "andres",
                    splits = listOf(
                        ExpenseSplit(userId = "andres", amountCents = 15000L),
                        ExpenseSplit(userId = "antonio", amountCents = 15000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                expenses = expenses,
                memberIds = listOf("andres", "antonio")
            )
            val balanceMap = result.associateBy { it.userId }

            // Andrés: contributed=30000, nonCashSpent=15000
            // pocketBalance = 30000 - 0 - 15000 = 15000
            assertEquals(15000L, balanceMap["andres"]!!.pocketBalance)

            // Antonio: contributed=0, nonCashSpent=15000
            // pocketBalance = 0 - 0 - 15000 = -15000
            assertEquals(-15000L, balanceMap["antonio"]!!.pocketBalance)
        }

        @Test
        fun `personal expense - payer net zero`() {
            // Andrés pays €15 personal expense (only he is in the split).
            // Paired contribution: 1500, expense split: 1500 to Andrés only.
            val contributions = listOf(
                Contribution(
                    userId = "andres",
                    amount = 1500L,
                    contributionScope = PayerType.USER,
                    linkedExpenseId = "exp-personal"
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-personal",
                    sourceAmount = 1500L,
                    groupAmount = 1500L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    payerType = PayerType.USER,
                    payerId = "andres",
                    splits = listOf(
                        ExpenseSplit(userId = "andres", amountCents = 1500L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                expenses = expenses,
                memberIds = twoMembers
            )
            val balanceMap = result.associateBy { it.userId }

            // Andrés: contributed=1500, nonCashSpent=1500
            // pocketBalance = 1500 - 0 - 1500 = 0
            assertEquals(0L, balanceMap["andres"]!!.pocketBalance)

            // Maria: not involved at all
            assertEquals(0L, balanceMap["maria"]!!.pocketBalance)
        }
    }
}
