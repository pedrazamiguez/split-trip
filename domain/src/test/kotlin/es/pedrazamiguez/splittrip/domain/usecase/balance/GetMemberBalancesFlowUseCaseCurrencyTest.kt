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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetMemberBalancesFlowUseCase — Per-Currency Breakdown")
class GetMemberBalancesFlowUseCaseCurrencyTest {

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
    @DisplayName("Per-currency breakdown")
    inner class PerCurrencyBreakdown {

        private val twoMembers = listOf("user-1", "user-2")

        @Test
        fun `single-currency EUR withdrawal produces cashInHandByCurrency with EUR entry`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    amountWithdrawn = 10000L,
                    remainingAmount = 10000L,
                    currency = "EUR",
                    deductedBaseAmount = 10000L
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(1, u1.cashInHandByCurrency.size)
            val eurEntry = u1.cashInHandByCurrency[0]
            assertEquals("EUR", eurEntry.currency)
            assertEquals(5000L, eurEntry.amountCents) // 10000 / 2 members
            assertEquals(5000L, eurEntry.equivalentCents) // same as native for group currency
        }

        @Test
        fun `foreign currency THB withdrawal tracks native and equivalent amounts`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    amountWithdrawn = 1000000L, // 10000 THB
                    remainingAmount = 1000000L,
                    currency = "THB",
                    deductedBaseAmount = 2683L // ~26.83 EUR
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(1, u1.cashInHandByCurrency.size)
            val thbEntry = u1.cashInHandByCurrency[0]
            assertEquals("THB", thbEntry.currency)
            assertEquals(500000L, thbEntry.amountCents) // 1000000 / 2 = 500000 THB cents
            // Equivalent: proportional = 500000 * 1342 / 1000000 ≈ 671 (rounding varies)
            // deducted 2683 split to 1342 + 1341 (remainder goes to first), native 500000 each
            assertTrue(thbEntry.equivalentCents > 0)
        }

        @Test
        fun `cashInHandByCurrency subtracts cash expenses per currency`() {
            // After FIFO processes the 4500 THB cash expense, withdrawal.remainingAmount = 95500.
            // The balance engine reads remainingAmount directly (sum-of-remaining approach).
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 100000L, // 1000 THB
                    remainingAmount = 95500L, // post-FIFO: 100000 - 4500 = 95500 THB
                    currency = "THB",
                    deductedBaseAmount = 2700L // 27 EUR
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 4500L, // 45 THB
                    sourceCurrency = "THB",
                    groupAmount = 121L, // 1.21 EUR
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 4500L)
                    )
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(1, u1.cashInHandByCurrency.size)
            val thbEntry = u1.cashInHandByCurrency[0]
            assertEquals("THB", thbEntry.currency)
            assertEquals(95500L, thbEntry.amountCents) // remainingAmount = 95500 THB
            assertTrue(thbEntry.equivalentCents > 0) // proportional equivalent
        }

        @Test
        fun `cashInHandByCurrency excludes currency with zero remaining`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 4500L,
                    remainingAmount = 0L,
                    currency = "THB",
                    deductedBaseAmount = 121L
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 4500L,
                    sourceCurrency = "THB",
                    groupAmount = 121L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 4500L)
                    )
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertTrue(u1.cashInHandByCurrency.isEmpty())
        }

        @Test
        fun `cashSpentByCurrency tracks cash expenses per source currency`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 4500L,
                    sourceCurrency = "THB",
                    groupAmount = 121L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 2250L),
                        ExpenseSplit(userId = "user-2", amountCents = 2250L)
                    )
                )
            )
            val result = compute(
                expenses = expenses,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(1, u1.cashSpentByCurrency.size)
            assertEquals("THB", u1.cashSpentByCurrency[0].currency)
            assertEquals(2250L, u1.cashSpentByCurrency[0].amountCents)
            // Equivalent: 2250 * 121 / 4500 = 61 (rounded)
            assertEquals(61L, u1.cashSpentByCurrency[0].equivalentCents)
        }

        @Test
        fun `nonCashSpentByCurrency tracks non-cash expenses per source currency`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 2000L,
                    sourceCurrency = "EUR",
                    groupAmount = 2000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 1000L),
                        ExpenseSplit(userId = "user-2", amountCents = 1000L)
                    )
                )
            )
            val result = compute(
                expenses = expenses,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(1, u1.nonCashSpentByCurrency.size)
            assertEquals("EUR", u1.nonCashSpentByCurrency[0].currency)
            assertEquals(1000L, u1.nonCashSpentByCurrency[0].amountCents)
            // Same currency → equivalent equals native
            assertEquals(1000L, u1.nonCashSpentByCurrency[0].equivalentCents)
        }

        @Test
        fun `multi-currency expenses produce separate entries per currency`() {
            val expenses = listOf(
                Expense(
                    id = "exp-eur",
                    sourceAmount = 2000L,
                    sourceCurrency = "EUR",
                    groupAmount = 2000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 1000L)
                    )
                ),
                Expense(
                    id = "exp-thb",
                    sourceAmount = 100000L,
                    sourceCurrency = "THB",
                    groupAmount = 2700L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 50000L)
                    )
                )
            )
            val result = compute(
                expenses = expenses,
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(2, u1.nonCashSpentByCurrency.size)
            val currencies = u1.nonCashSpentByCurrency.map { it.currency }
            assertTrue("EUR" in currencies)
            assertTrue("THB" in currencies)
            val eurEntry = u1.nonCashSpentByCurrency.first { it.currency == "EUR" }
            assertEquals(1000L, eurEntry.amountCents)
            assertEquals(1000L, eurEntry.equivalentCents)
            val thbEntry = u1.nonCashSpentByCurrency.first { it.currency == "THB" }
            assertEquals(50000L, thbEntry.amountCents)
            // Equivalent: 50000 * 2700 / 100000 = 1350
            assertEquals(1350L, thbEntry.equivalentCents)
        }

        @Test
        fun `no expenses and no withdrawals produce empty per-currency lists`() {
            val result = compute(
                memberIds = twoMembers,
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertTrue(u1.cashInHandByCurrency.isEmpty())
            assertTrue(u1.cashSpentByCurrency.isEmpty())
            assertTrue(u1.nonCashSpentByCurrency.isEmpty())
        }

        @Test
        @Suppress("LongMethod") // Integration test scenario — length is test data + assertions, not logic
        fun `full scenario from issue matches expected per-member per-currency breakdown`() {
            // EUR group, THB additional currency, two members (50/50 couple)
            val subunit = Subunit(
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
                    subunitId = "sub-couple",
                    amount = 10000L // 100 EUR for couple → 50 each
                )
            )
            val withdrawals = listOf(
                // 1000 THB (26.83 EUR) for couple → ~13.41/13.42 each
                // Post-FIFO: user-2 spends 4500 THB coffee from this pool → remainingAmount = 95500
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "sub-couple",
                    amountWithdrawn = 100000L, // 1000 THB in cents
                    remainingAmount = 95500L, // post-FIFO: 100000 - 4500 (coffee) = 95500
                    currency = "THB",
                    deductedBaseAmount = 2683L
                ),
                // 10 EUR personal withdrawal by user-1 (not consumed)
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 1000L, // 10 EUR
                    remainingAmount = 1000L,
                    currency = "EUR",
                    deductedBaseAmount = 1000L
                )
            )
            val expenses = listOf(
                // 45 THB (1.21 EUR) CASH expense for user-2
                Expense(
                    id = "exp-coffee",
                    sourceAmount = 4500L,
                    sourceCurrency = "THB",
                    groupAmount = 121L,
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-2", amountCents = 4500L)
                    )
                )
            )

            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                subunits = listOf(subunit),
                memberIds = listOf("user-1", "user-2"),
                groupCurrency = "EUR"
            )
            val balanceMap = result.associateBy { it.userId }

            // User-1: withdrew 10 EUR personal + share of THB couple pool
            val u1 = balanceMap["user-1"]!!
            // cashInHandByCurrency: EUR (1000 cents) + THB (47750 cents = 95500 remaining / 2 members)
            assertEquals(2, u1.cashInHandByCurrency.size)
            val u1Eur = u1.cashInHandByCurrency.first { it.currency == "EUR" }
            assertEquals(1000L, u1Eur.amountCents)
            assertEquals(1000L, u1Eur.equivalentCents) // same currency
            val u1Thb = u1.cashInHandByCurrency.first { it.currency == "THB" }
            assertEquals(47750L, u1Thb.amountCents) // 95500 remaining / 2 members = 47750
            assertTrue(u1Thb.equivalentCents > 0) // proportional from deductedBaseAmount
            assertTrue(u1.cashSpentByCurrency.isEmpty()) // no cash expenses for user-1

            // User-2: has 50% share of THB couple pool; user-2's coffee reduced pool to 95500
            val u2 = balanceMap["user-2"]!!
            // cashInHandByCurrency: THB with 95500 remaining / 2 members = 47750
            assertEquals(1, u2.cashInHandByCurrency.size)
            assertEquals("THB", u2.cashInHandByCurrency[0].currency)
            assertEquals(47750L, u2.cashInHandByCurrency[0].amountCents)
            // cashSpentByCurrency: THB 4500
            assertEquals(1, u2.cashSpentByCurrency.size)
            assertEquals("THB", u2.cashSpentByCurrency[0].currency)
            assertEquals(4500L, u2.cashSpentByCurrency[0].amountCents)
        }

        @Test
        fun `per-currency lists are sorted alphabetically by currency code`() {
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 5000L,
                    remainingAmount = 5000L,
                    currency = "USD",
                    deductedBaseAmount = 4500L
                ),
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 1000L,
                    remainingAmount = 1000L,
                    currency = "EUR",
                    deductedBaseAmount = 1000L
                ),
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 100000L,
                    remainingAmount = 100000L,
                    currency = "THB",
                    deductedBaseAmount = 2700L
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                memberIds = listOf("user-1"),
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(3, u1.cashInHandByCurrency.size)
            assertEquals("EUR", u1.cashInHandByCurrency[0].currency)
            assertEquals("THB", u1.cashInHandByCurrency[1].currency)
            assertEquals("USD", u1.cashInHandByCurrency[2].currency)
        }
    }
}
