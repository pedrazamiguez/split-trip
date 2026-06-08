package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
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

@DisplayName("GetMemberBalancesFlowUseCase — Add-On Integration")
class GetMemberBalancesFlowUseCaseAddOnTest {

    private lateinit var useCase: GetMemberBalancesFlowUseCase
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
    @DisplayName("Add-on integration")
    inner class AddOnIntegration {
        private val twoMembers = listOf("user-1", "user-2")

        @Test
        fun `expenses without add-ons use base group amount`() {
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L),
                        ExpenseSplit(userId = "user-2", amountCents = 5000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            assertEquals(5000L, u1.nonCashSpent)
            assertEquals(5000L, u2.nonCashSpent)
        }

        @Test
        fun `ON_TOP fee increases effective group amount for balance calculation`() {
            // Base: 100 EUR, Fee: 2.50 EUR ON_TOP → Effective: 102.50 EUR
            val expenses = listOf(
                Expense(
                    id = "exp-with-fee",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "fee-1",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 250L,
                            groupAmountCents = 250L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L),
                        ExpenseSplit(userId = "user-2", amountCents = 5000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            // Effective: 10250L. Each split is 5000/10000 * 10250 = 5125
            assertEquals(5125L, u1.nonCashSpent)
            assertEquals(5125L, u2.nonCashSpent)
        }

        @Test
        fun `ON_TOP tip increases effective group amount`() {
            // Base: 60 EUR, Tip: 10 EUR ON_TOP → Effective: 70 EUR
            val expenses = listOf(
                Expense(
                    id = "exp-with-tip",
                    sourceAmount = 6000L,
                    groupAmount = 6000L,
                    paymentMethod = PaymentMethod.CASH,
                    addOns = listOf(
                        AddOn(
                            id = "tip-1",
                            type = AddOnType.TIP,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 1000L,
                            groupAmountCents = 1000L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3000L),
                        ExpenseSplit(userId = "user-2", amountCents = 3000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            // Effective: 7000L. Each split is 3000/6000 * 7000 = 3500
            val u1 = result.first { it.userId == "user-1" }
            assertEquals(3500L, u1.cashSpent)
        }

        @Test
        fun `INCLUDED tip reconstructs effective group amount from base cost`() {
            // User entered 72 EUR total with 9% tip included.
            // Base cost: 7200 / 1.09 = 6605.50... → HALF_UP → 6606 cents (66.06 EUR)
            // Residual (tip): 7200 − 0 − 6606 = 594 cents  (conservation of currency)
            // Effective: 6606 + 594 = 7200 cents = 72.00 EUR (exactly the original total)
            val expenses = listOf(
                Expense(
                    id = "exp-included-tip",
                    sourceAmount = 6606L,
                    groupAmount = 6606L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "tip-1",
                            type = AddOnType.TIP,
                            mode = AddOnMode.INCLUDED,
                            amountCents = 594L,
                            groupAmountCents = 594L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 3303L),
                        ExpenseSplit(userId = "user-2", amountCents = 3303L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            // Effective = 7200. Each split = 3303/6606 * 7200 = 3600.0 (exact)
            assertEquals(3600L, u1.nonCashSpent)
            assertEquals(3600L, u2.nonCashSpent)
        }

        @Test
        fun `DISCOUNT reduces effective group amount`() {
            // Base: 100 EUR, Discount: 5 EUR → Effective: 95 EUR
            val expenses = listOf(
                Expense(
                    id = "exp-with-discount",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "discount-1",
                            type = AddOnType.DISCOUNT,
                            mode = AddOnMode.ON_TOP, // mode doesn't matter for discounts
                            amountCents = 500L,
                            groupAmountCents = 500L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L),
                        ExpenseSplit(userId = "user-2", amountCents = 5000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            // Effective: 9500L. Each split is 5000/10000 * 9500 = 4750
            assertEquals(4750L, u1.nonCashSpent)
            assertEquals(4750L, u2.nonCashSpent)
        }

        @Test
        fun `mixed add-ons combine correctly for balance calculation`() {
            // Base: 100 EUR, Fee: 10 EUR ON_TOP, Tip: 5 EUR ON_TOP, Discount: 2 EUR
            // Effective: 100 + 10 + 5 - 2 = 113 EUR
            val expenses = listOf(
                Expense(
                    id = "exp-mixed",
                    sourceAmount = 10000L,
                    groupAmount = 10000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "fee-1",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 1000L,
                            groupAmountCents = 1000L
                        ),
                        AddOn(
                            id = "tip-1",
                            type = AddOnType.TIP,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L,
                            groupAmountCents = 500L
                        ),
                        AddOn(
                            id = "discount-1",
                            type = AddOnType.DISCOUNT,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 200L,
                            groupAmountCents = 200L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 4000L),
                        ExpenseSplit(userId = "user-2", amountCents = 6000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers)
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            // Effective: 11300L
            // user-1: 4000/10000 * 11300 = 4520
            // user-2: 6000/10000 * 11300 = 6780
            assertEquals(4520L, u1.nonCashSpent)
            assertEquals(6780L, u2.nonCashSpent)
        }

        @Test
        fun `foreign currency expense with ON_TOP fee scales correctly to group currency`() {
            // 200 THB boat ride with 9.25 THB ON_TOP booking fee
            // Exchange rate: 1 EUR = 37.22 THB → groupAmount = 5.37 EUR (537 cents)
            // Fee groupAmount: 25 cents (9.25/37.22)
            // Effective: 537 + 25 = 562 EUR cents
            val expenses = listOf(
                Expense(
                    id = "exp-boat-thb",
                    sourceAmount = 20000L, // 200 THB
                    sourceCurrency = "THB",
                    groupAmount = 537L, // 5.37 EUR
                    groupCurrency = "EUR",
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "fee-1",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 925L, // 9.25 THB
                            currency = "THB",
                            groupAmountCents = 25L // 0.25 EUR
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 10000L),
                        ExpenseSplit(userId = "user-2", amountCents = 10000L)
                    )
                )
            )
            val result = compute(expenses = expenses, memberIds = twoMembers, groupCurrency = "EUR")
            val u1 = result.first { it.userId == "user-1" }
            val u2 = result.first { it.userId == "user-2" }
            // Effective: 562L. Each split is 10000/20000 * 562 = 281
            assertEquals(281L, u1.nonCashSpent)
            assertEquals(281L, u2.nonCashSpent)
            // Total: 562
            assertEquals(562L, u1.nonCashSpent + u2.nonCashSpent)
        }

        @Test
        fun `pocketBalance reflects add-on adjusted expenses`() {
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 10000L)
            )
            val expenses = listOf(
                Expense(
                    id = "exp-1",
                    sourceAmount = 6000L,
                    groupAmount = 6000L,
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    addOns = listOf(
                        AddOn(
                            id = "fee-1",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 200L,
                            groupAmountCents = 200L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 6000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                expenses = expenses,
                memberIds = listOf("user-1")
            )
            val u1 = result.first { it.userId == "user-1" }
            // Effective expense: 6200L
            // pocketBalance = contributed - withdrawn - nonCashSpent = 10000 - 0 - 6200 = 3800
            assertEquals(6200L, u1.nonCashSpent)
            assertEquals(3800L, u1.pocketBalance)
        }

        @Test
        fun `cash expense with add-on affects cashSpent correctly`() {
            // After FIFO processes the 5500L effective cash expense (5000 base + 500 tip),
            // the withdrawal's remainingAmount is reduced from 10000 to 4500.
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 10000L,
                    remainingAmount = 4500L, // post-FIFO: 10000 - 5500 (base + tip) = 4500
                    currency = "EUR",
                    deductedBaseAmount = 10000L
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-cash",
                    sourceAmount = 5000L,
                    groupAmount = 5000L,
                    paymentMethod = PaymentMethod.CASH,
                    addOns = listOf(
                        AddOn(
                            id = "tip-1",
                            type = AddOnType.TIP,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L,
                            groupAmountCents = 500L
                        )
                    ),
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 5000L)
                    )
                )
            )
            val result = compute(
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = listOf("user-1")
            )
            val u1 = result.first { it.userId == "user-1" }
            // Effective: 5500L cash spent
            assertEquals(5500L, u1.cashSpent)
            // cashInHand = remainingAmount × (deductedBaseAmount / amountWithdrawn) = 4500 × 1.0 = 4500
            assertEquals(4500L, u1.cashInHand)
        }

        @Test
        fun `ATM fee add-on on withdrawal increases effective withdrawn amount`() {
            // Withdrawal of 10000 THB with 5 EUR ATM fee (706 cents in group currency)
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 50000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 1000000L, // 10,000 THB in cents
                    remainingAmount = 1000000L,
                    currency = "THB",
                    deductedBaseAmount = 27000L, // 270 EUR deducted from pocket
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-1",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L, // 5 EUR fee charged
                            currency = "EUR",
                            groupAmountCents = 500L // Already in group currency
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                memberIds = listOf("user-1")
            )
            val u1 = result.first { it.userId == "user-1" }
            // Effective withdrawn = deductedBaseAmount + ATM fee groupAmountCents = 27000 + 500 = 27500
            assertEquals(27500L, u1.withdrawn)
            // pocketBalance = contributed - withdrawn - nonCashSpent = 50000 - 27500 - 0 = 22500
            assertEquals(22500L, u1.pocketBalance)
        }

        @Test
        fun `GROUP-scoped withdrawal with ATM fee distributes effective amount equally`() {
            val contributions = groupMemberIds.map {
                Contribution(userId = it, contributionScope = PayerType.USER, amount = 10000L)
            }
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    amountWithdrawn = 500000L, // 5000 THB
                    remainingAmount = 500000L,
                    currency = "THB",
                    deductedBaseAmount = 13587L, // ~135.87 EUR
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-2",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 706L, // ~7.06 EUR fee in group currency
                            currency = "EUR",
                            groupAmountCents = 706L
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                memberIds = groupMemberIds
            )
            // Effective deducted = 13587 + 706 = 14293
            // Per member = 14293 / 4 = 3573 (with remainder distribution)
            val totalEffectiveWithdrawn = result.sumOf { it.withdrawn }
            assertEquals(14293L, totalEffectiveWithdrawn)
            // Each member's withdrawn should be ~3573 (3573 * 3 + 3574 = 14293)
            val balanceMap = result.associateBy { it.userId }
            assertTrue(balanceMap.values.all { it.withdrawn in 3573L..3574L })
        }

        @Test
        fun `SUBUNIT-scoped withdrawal with ATM fee distributes by member shares`() {
            // Couple subunit: user-1 and user-2 with 50-50 shares
            val coupleSubunit = Subunit(
                id = "couple-1",
                name = "Couple",
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 20000L),
                Contribution(userId = "user-2", contributionScope = PayerType.USER, amount = 20000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "couple-1",
                    amountWithdrawn = 1000000L, // 10,000 THB
                    remainingAmount = 1000000L,
                    currency = "THB",
                    deductedBaseAmount = 27000L, // 270 EUR
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-3",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L, // 5 EUR fee
                            currency = "EUR",
                            groupAmountCents = 500L
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                subunits = listOf(coupleSubunit),
                memberIds = listOf("user-1", "user-2", "user-3", "user-4")
            )
            val balanceMap = result.associateBy { it.userId }

            // Effective deducted = 27000 + 500 = 27500
            // Split 50-50 between user-1 and user-2: 13750 each
            assertEquals(13750L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(13750L, balanceMap["user-2"]!!.withdrawn)
            // user-3 and user-4 are not in the subunit, so they get nothing
            assertEquals(0L, balanceMap["user-3"]!!.withdrawn)
            assertEquals(0L, balanceMap["user-4"]!!.withdrawn)

            // pocketBalance for user-1 = 20000 - 13750 = 6250
            assertEquals(6250L, balanceMap["user-1"]!!.pocketBalance)
            assertEquals(6250L, balanceMap["user-2"]!!.pocketBalance)
        }

        @Test
        fun `issue 679 - ATM fee does NOT inflate cashInHand scalar`() {
            // Exact reproduction from issue #679:
            // Contribution = 500 EUR, Withdrawal = 162 EUR with 1.25 EUR ATM fee, no expenses.
            // cashInHand should be 162 EUR (physical cash), NOT 163.25 EUR (effective deducted).
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 50000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 16200L, // 162 EUR in cents
                    remainingAmount = 16200L,
                    currency = "EUR",
                    deductedBaseAmount = 16200L, // 162 EUR deducted from pocket (base)
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-679",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 125L, // 1.25 EUR ATM fee
                            currency = "EUR",
                            groupAmountCents = 125L
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                memberIds = listOf("user-1"),
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            // withdrawn (effective) = 16200 + 125 = 16325 (includes ATM fee for pocket math)
            assertEquals(16325L, u1.withdrawn)
            // cashInHand = rawWithdrawn - cashSpent = 16200 - 0 = 16200 (physical cash only)
            assertEquals(16200L, u1.cashInHand)
            // pocketBalance = 50000 - 16325 - 0 = 33675
            assertEquals(33675L, u1.pocketBalance)
            // cashInHandByCurrency should agree with the scalar
            assertEquals(1, u1.cashInHandByCurrency.size)
            assertEquals(16200L, u1.cashInHandByCurrency[0].amountCents)
        }

        @Test
        fun `ATM fee excluded from cashInHand with cash expense partially spent`() {
            // Withdrawal of 10000 THB (270 EUR base) with 5 EUR ATM fee.
            // Cash expense spent 5000 THB (half).
            // cashInHand should be 270 EUR - 135 EUR = 135 EUR (not 275 - 135 = 140).
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 50000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.USER,
                    amountWithdrawn = 1000000L, // 10,000 THB
                    remainingAmount = 500000L, // 5,000 THB remaining
                    currency = "THB",
                    deductedBaseAmount = 27000L, // 270 EUR
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-partial",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L, // 5 EUR fee
                            currency = "EUR",
                            groupAmountCents = 500L
                        )
                    )
                )
            )
            val expenses = listOf(
                Expense(
                    id = "exp-cash-thb",
                    sourceAmount = 500000L, // 5000 THB
                    sourceCurrency = "THB",
                    groupAmount = 13500L, // 135 EUR equivalent
                    paymentMethod = PaymentMethod.CASH,
                    splits = listOf(
                        ExpenseSplit(userId = "user-1", amountCents = 500000L)
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                memberIds = listOf("user-1"),
                groupCurrency = "EUR"
            )
            val u1 = result.first { it.userId == "user-1" }
            // withdrawn (effective) = 27000 + 500 = 27500
            assertEquals(27500L, u1.withdrawn)
            // cashSpent = 13500 EUR equivalent of THB cash expense
            assertEquals(13500L, u1.cashSpent)
            // cashInHand = rawWithdrawn - cashSpent = 27000 - 13500 = 13500 (no ATM fee)
            assertEquals(13500L, u1.cashInHand)
            // pocketBalance = 50000 - 27500 - 0 = 22500
            assertEquals(22500L, u1.pocketBalance)

            // Verify remaining THB cash equivalent excludes ATM fee at per-currency level.
            val nonGroupCurrencyCash = u1.cashInHandByCurrency.filter { it.currency != "EUR" }
            assertEquals(1, nonGroupCurrencyCash.size)
            val thbCash = nonGroupCurrencyCash.single()
            // Remaining THB equivalent should be 13500 EUR (raw withdrawn 27000 - spent 13500),
            // not 13750 (which would incorrectly include part of the ATM fee).
            assertEquals(13500L, thbCash.equivalentCents)
        }

        @Test
        fun `GROUP-scoped withdrawal ATM fee excluded from per-member cashInHand`() {
            val contributions = groupMemberIds.map {
                Contribution(userId = it, contributionScope = PayerType.USER, amount = 10000L)
            }
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.GROUP,
                    amountWithdrawn = 500000L, // 5000 THB
                    remainingAmount = 500000L,
                    currency = "THB",
                    deductedBaseAmount = 13587L, // ~135.87 EUR
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-group",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 706L, // ~7.06 EUR fee
                            currency = "EUR",
                            groupAmountCents = 706L
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                memberIds = groupMemberIds,
                groupCurrency = "EUR"
            )
            // Effective deducted = 13587 + 706 = 14293, per member ~3573
            // Raw deducted = 13587, per member = 13587/4 = 3396 (with remainder)
            val totalEffectiveWithdrawn = result.sumOf { it.withdrawn }
            assertEquals(14293L, totalEffectiveWithdrawn)
            // Total raw cashInHand should equal raw deducted (no expenses)
            val totalCashInHand = result.sumOf { it.cashInHand }
            assertEquals(13587L, totalCashInHand)
            // Verify per-member: cashInHand < withdrawn (ATM fee excluded from cashInHand)
            result.forEach { balance ->
                assertTrue(
                    balance.cashInHand <= balance.withdrawn,
                    "cashInHand (${balance.cashInHand}) should be <= withdrawn (${balance.withdrawn})"
                )
            }
        }

        @Test
        fun `SUBUNIT-scoped withdrawal ATM fee excluded from per-member cashInHand`() {
            val coupleSubunit = Subunit(
                id = "couple-1",
                name = "Couple",
                memberIds = listOf("user-1", "user-2"),
                memberShares = mapOf(
                    "user-1" to BigDecimal("0.5"),
                    "user-2" to BigDecimal("0.5")
                )
            )
            val contributions = listOf(
                Contribution(userId = "user-1", contributionScope = PayerType.USER, amount = 20000L),
                Contribution(userId = "user-2", contributionScope = PayerType.USER, amount = 20000L)
            )
            val withdrawals = listOf(
                CashWithdrawal(
                    withdrawnBy = "user-1",
                    withdrawalScope = PayerType.SUBUNIT,
                    subunitId = "couple-1",
                    amountWithdrawn = 1000000L, // 10,000 THB
                    remainingAmount = 1000000L,
                    currency = "THB",
                    deductedBaseAmount = 27000L, // 270 EUR
                    addOns = listOf(
                        AddOn(
                            id = "atm-fee-sub",
                            type = AddOnType.FEE,
                            mode = AddOnMode.ON_TOP,
                            amountCents = 500L, // 5 EUR fee
                            currency = "EUR",
                            groupAmountCents = 500L
                        )
                    )
                )
            )
            val result = compute(
                contributions = contributions,
                withdrawals = withdrawals,
                subunits = listOf(coupleSubunit),
                memberIds = listOf("user-1", "user-2", "user-3", "user-4"),
                groupCurrency = "EUR"
            )
            val balanceMap = result.associateBy { it.userId }

            // Effective deducted = 27000 + 500 = 27500, split 50-50: 13750 each
            assertEquals(13750L, balanceMap["user-1"]!!.withdrawn)
            assertEquals(13750L, balanceMap["user-2"]!!.withdrawn)

            // Raw deducted = 27000, split 50-50: 13500 each
            // cashInHand = rawWithdrawn - cashSpent = 13500 - 0 = 13500
            assertEquals(13500L, balanceMap["user-1"]!!.cashInHand)
            assertEquals(13500L, balanceMap["user-2"]!!.cashInHand)

            // Non-subunit members: zero
            assertEquals(0L, balanceMap["user-3"]!!.cashInHand)
            assertEquals(0L, balanceMap["user-4"]!!.cashInHand)
        }
    }
}
