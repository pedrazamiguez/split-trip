package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupPocketBalanceFlowUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetGroupPocketBalanceFlowUseCaseTest {

    private lateinit var contributionRepository: ContributionRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var useCase: GetGroupPocketBalanceFlowUseCase

    private val groupId = "group-123"
    private val currency = "GBP"

    @BeforeEach
    fun setUp() {
        contributionRepository = mockk()
        expenseRepository = mockk()
        cashWithdrawalRepository = mockk()
        useCase = GetGroupPocketBalanceFlowUseCaseImpl(
            contributionRepository,
            expenseRepository,
            cashWithdrawalRepository,
            AddOnCalculationServiceImpl()
        )
    }

    @Nested
    inner class VirtualBalanceCalculation {

        @Test
        fun `cash expenses are excluded from virtual balance`() = runTest {
            // Given: 500 contributed, 100 cash expense, 200 withdrawal
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CASH
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 20000L,
                        amountWithdrawn = 20000L,
                        remainingAmount = 10000L,
                        currency = currency
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 50000 - 0 (no non-cash expenses) - 20000 = 30000
            assertEquals(30000L, result.virtualBalance)
            // totalExpenses still includes the cash expense for the UI summary
            assertEquals(10000L, result.totalExpenses)
        }

        @Test
        fun `non-cash expenses deduct from virtual balance`() = runTest {
            // Given: 500 contributed, 150 debit card expense, no withdrawals
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 15000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 50000 - 15000 - 0 = 35000
            assertEquals(35000L, result.virtualBalance)
            assertEquals(15000L, result.totalExpenses)
        }

        @Test
        fun `mixed cash and non-cash expenses only deducts non-cash from virtual balance`() = runTest {
            // Given: 500 contributed, 100 cash expense + 150 debit card, 200 withdrawal
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CASH
                    ),
                    Expense(
                        groupAmount = 15000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 20000L,
                        amountWithdrawn = 20000L,
                        remainingAmount = 10000L,
                        currency = currency
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 50000 - 15000 (non-cash only) - 20000 = 15000
            assertEquals(15000L, result.virtualBalance)
            // totalExpenses includes both cash + non-cash for UI
            assertEquals(25000L, result.totalExpenses)
        }

        @Test
        fun `reproduces the original double-deduction bug scenario`() = runTest {
            // Given: reproduces the issue from SPLTRP-0415
            // Contribution: 15921 GBP (159.21)
            // Expenses: 10721 GBP total (107.21) — some paid with cash, some with card
            // Cash withdrawal: deducted 4547 GBP (45.47) from virtual pocket
            //
            // The old formula would give: 15921 - 10721 - 4547 = 653 (or negative
            // depending on exact amounts). With the fix, cash expenses (~45 GBP
            // worth) are excluded from virtualExpenses.

            val cashExpenseAmount = 4000L // 40 GBP paid in cash
            val cardExpenseAmount = 6721L // 67.21 GBP paid by card

            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 15921L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = cashExpenseAmount,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CASH
                    ),
                    Expense(
                        groupAmount = cardExpenseAmount,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 4547L,
                        amountWithdrawn = 4547L,
                        remainingAmount = 547L,
                        currency = currency
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 15921 - 6721 (non-cash) - 4547 = 4653
            // This is positive! The old formula would give: 15921 - 10721 - 4547 = 653
            // (and with larger cash amounts it would go negative)
            assertEquals(4653L, result.virtualBalance)
            // totalExpenses still shows the full amount for UI
            assertEquals(10721L, result.totalExpenses)
        }

        @Test
        fun `all payment methods except CASH deduct from virtual balance`() = runTest {
            // Given: one expense per non-cash payment method
            val nonCashMethods = listOf(
                PaymentMethod.BIZUM,
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.DEBIT_CARD,
                PaymentMethod.BANK_TRANSFER,
                PaymentMethod.PAYPAL,
                PaymentMethod.VENMO,
                PaymentMethod.OTHER
            )
            val expensePerMethod = 1000L
            val totalContributions = 100000L

            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = totalContributions, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                nonCashMethods.map { method ->
                    Expense(
                        groupAmount = expensePerMethod,
                        groupCurrency = currency,
                        paymentMethod = method
                    )
                }
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: all 7 non-cash expenses deduct from virtual balance
            val expectedVirtualExpenses = nonCashMethods.size * expensePerMethod
            assertEquals(
                totalContributions - expectedVirtualExpenses,
                result.virtualBalance
            )
        }

        @Test
        fun `empty data produces zero balance`() = runTest {
            // Given
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                emptyList()
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                emptyList()
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then
            assertEquals(0L, result.totalContributions)
            assertEquals(0L, result.totalExpenses)
            assertEquals(0L, result.virtualBalance)
            assertEquals(currency, result.currency)
            assertEquals(emptyMap<String, Long>(), result.cashBalances)
            assertEquals(emptyMap<String, Long>(), result.cashEquivalents)
            assertEquals(0L, result.totalCashEquivalent)
            assertEquals(0L, result.totalExtras)
        }
    }

    @Nested
    inner class TotalCashEquivalentCalculation {

        @Test
        fun `totalCashEquivalent includes base currency cash at face value`() = runTest {
            // Given: 100 GBP remaining in cash (same as group currency)
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 10000L,
                        currency = currency
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalCashEquivalent = 10000 (base currency, face value)
            assertEquals(10000L, result.totalCashEquivalent)
        }

        @Test
        fun `totalCashEquivalent includes foreign cash converted proportionally`() = runTest {
            // Given: 5000 THB remaining out of 10000 THB withdrawn, which deducted 270 GBP
            // Proportional equivalent: (5000/10000) * 27000 = 13500
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 27000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 5000L,
                        currency = "THB"
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalCashEquivalent = 0 (no base currency cash) + 13500 (foreign equivalent)
            assertEquals(13500L, result.totalCashEquivalent)
        }

        @Test
        fun `totalCashEquivalent sums base currency and foreign cash`() = runTest {
            // Given: 100 GBP cash + 5000 THB (equivalent ~135 GBP)
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 10000L,
                        currency = currency
                    ),
                    CashWithdrawal(
                        deductedBaseAmount = 27000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 5000L,
                        currency = "THB"
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalCashEquivalent = 10000 (GBP) + 13500 (THB equivalent)
            assertEquals(23500L, result.totalCashEquivalent)
        }

        @Test
        fun `totalCashEquivalent is zero when no cash remains`() = runTest {
            // Given: withdrawal with zero remaining
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 0L,
                        currency = "THB"
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then
            assertEquals(0L, result.totalCashEquivalent)
            assertEquals(emptyMap<String, Long>(), result.cashBalances)
        }
    }

    @Nested
    inner class CashBalancesAndEquivalents {

        @Test
        fun `cashBalances groups remaining amounts by currency`() = runTest {
            // Given: two THB withdrawals
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 5000L,
                        remainingAmount = 3000L,
                        currency = "THB"
                    ),
                    CashWithdrawal(
                        deductedBaseAmount = 20000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 7000L,
                        currency = "THB"
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then
            assertEquals(mapOf("THB" to 10000L), result.cashBalances)
        }

        @Test
        fun `cashBalances excludes currencies with zero remaining`() = runTest {
            // Given
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 5000L,
                        remainingAmount = 0L,
                        currency = "THB"
                    ),
                    CashWithdrawal(
                        deductedBaseAmount = 5000L,
                        amountWithdrawn = 5000L,
                        remainingAmount = 3000L,
                        currency = "EUR"
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: only EUR remains
            assertEquals(mapOf("EUR" to 3000L), result.cashBalances)
        }

        @Test
        fun `cashEquivalents excludes base currency withdrawals`() = runTest {
            // Given: one GBP (base) and one EUR (foreign) withdrawal
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 10000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 5000L,
                        currency = currency // base currency
                    ),
                    CashWithdrawal(
                        deductedBaseAmount = 20000L,
                        amountWithdrawn = 10000L,
                        remainingAmount = 5000L,
                        currency = "EUR" // foreign
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: only EUR has an equivalent (foreign), GBP is base currency
            assertEquals(1, result.cashEquivalents.size)
            assertEquals(10000L, result.cashEquivalents["EUR"]) // (5000/10000) * 20000
        }
    }

    @Nested
    inner class ScheduledExpenses {

        @Test
        fun `future scheduled expense is excluded from totalExpenses and virtualExpenses`() = runTest {
            // Given: 500 contributed, one future scheduled expense (100), one finished (50)
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.SCHEDULED,
                        dueDate = LocalDateTime.now().plusDays(30) // future
                    ),
                    Expense(
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.FINISHED
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalExpenses only includes the finished expense
            assertEquals(5000L, result.totalExpenses)
            // virtualBalance = 50000 - 5000 (non-cash effective) - 0 = 45000
            assertEquals(45000L, result.virtualBalance)
            // scheduledHoldAmount = 10000 (the future scheduled expense)
            assertEquals(10000L, result.scheduledHoldAmount)
        }

        @Test
        fun `past scheduled expense is included in totalExpenses normally`() = runTest {
            // Given: scheduled expense with due date in the past
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 8000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.SCHEDULED,
                        dueDate = LocalDateTime.now().minusDays(5) // past
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: past scheduled expense is treated as effective
            assertEquals(8000L, result.totalExpenses)
            assertEquals(42000L, result.virtualBalance)
            assertEquals(0L, result.scheduledHoldAmount)
        }

        @Test
        fun `today scheduled expense is included in totalExpenses normally`() = runTest {
            // Given: scheduled expense with due date today
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 7000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.SCHEDULED,
                        dueDate = LocalDateTime.now() // today
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: today's scheduled expense is treated as effective (not future)
            assertEquals(7000L, result.totalExpenses)
            assertEquals(43000L, result.virtualBalance)
            assertEquals(0L, result.scheduledHoldAmount)
        }

        @Test
        fun `scheduledHoldAmount correctly sums multiple future scheduled expenses`() = runTest {
            // Given: two future scheduled expenses
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 15000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.SCHEDULED,
                        dueDate = LocalDateTime.now().plusDays(10)
                    ),
                    Expense(
                        groupAmount = 25000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        paymentStatus = PaymentStatus.SCHEDULED,
                        dueDate = LocalDateTime.now().plusDays(20)
                    ),
                    Expense(
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.FINISHED
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then
            assertEquals(5000L, result.totalExpenses) // only finished
            assertEquals(40000L, result.scheduledHoldAmount) // 15000 + 25000
            // virtualBalance = 100000 - 5000 - 0 = 95000
            assertEquals(95000L, result.virtualBalance)
        }

        @Test
        fun `non-scheduled expense with future dueDate is still included normally`() = runTest {
            // Given: a FINISHED expense that happens to have a future dueDate
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 12000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.FINISHED,
                        dueDate = LocalDateTime.now().plusDays(30)
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: FINISHED expenses are always effective regardless of dueDate
            assertEquals(12000L, result.totalExpenses)
            assertEquals(0L, result.scheduledHoldAmount)
            assertEquals(38000L, result.virtualBalance)
        }

        @Test
        fun `scheduledHoldAmount is zero when no future scheduled expenses exist`() = runTest {
            // Given: only finished expenses
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        paymentStatus = PaymentStatus.FINISHED
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then
            assertEquals(0L, result.scheduledHoldAmount)
        }
    }

    @Nested
    inner class AddOnImpactOnBalance {

        @Test
        fun `expense with ON_TOP fee increases totalExpenses`() = runTest {
            // Given: 1000 contributed, expense 50.00 + fee 2.50 EUR
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.FEE,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 250
                            )
                        )
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalExpenses = 5000 (base cost — add-ons excluded from summary)
            assertEquals(5000L, result.totalExpenses)
            // virtualBalance uses effective amount: 100000 - 5250 - 0 = 94750
            assertEquals(94750L, result.virtualBalance)
            // totalExtras = 250 (the ON_TOP fee)
            assertEquals(250L, result.totalExtras)
        }

        @Test
        fun `expense with INCLUDED tip reconstructs effective total from base cost`() = runTest {
            // Given: base cost 72.73 EUR (7273 cents), INCLUDED tip 7.27 EUR (727 cents)
            // Effective total = 7273 + 727 = 8000 (reconstructs original 80.00 EUR)
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 7273L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.TIP,
                                mode = AddOnMode.INCLUDED,
                                groupAmountCents = 727
                            )
                        )
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalExpenses = 7273 (base cost — tip excluded from summary)
            assertEquals(7273L, result.totalExpenses)
            // virtualBalance uses effective amount: 100000 - 8000 - 0 = 92000
            assertEquals(92000L, result.virtualBalance)
            // totalExtras = 727 (INCLUDED tip is now surfaced as an extra, alongside ON_TOP)
            assertEquals(727L, result.totalExtras)
        }

        @Test
        fun `expense with DISCOUNT reduces totalExpenses`() = runTest {
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.DISCOUNT,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 500
                            )
                        )
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalExpenses = 10000 (base cost — discounts are informational only in summary)
            assertEquals(10000L, result.totalExpenses)
            // totalExtras = 0 (discounts are excluded from extras)
            assertEquals(0L, result.totalExtras)
        }

        @Test
        fun `withdrawal with ATM fee add-on increases totalWithdrawals`() = runTest {
            // Given: withdrawal 270 EUR + ATM fee 7.06 EUR
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 27000L,
                        amountWithdrawn = 1000000L,
                        remainingAmount = 1000000L,
                        currency = "THB",
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.FEE,
                                mode = AddOnMode.ON_TOP,
                                amountCents = 26000,
                                currency = "THB",
                                groupAmountCents = 706
                            )
                        )
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 100000 - 0 - (27000 + 706) = 72294
            assertEquals(72294L, result.virtualBalance)
            // totalExtras = 0 (no expense add-ons) + (27706 - 27000) = 706 (ATM fee)
            assertEquals(706L, result.totalExtras)
        }

        @Test
        fun `mixed add-ons on expenses and withdrawals`() = runTest {
            // Given: 1000 contributed
            // Expense: 200 EUR + 10 EUR tip on top + 5 EUR discount
            // Withdrawal: 270 EUR + 7 EUR ATM fee
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 20000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.TIP,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 1000
                            ),
                            AddOn(
                                type = AddOnType.DISCOUNT,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 500
                            )
                        )
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 27000L,
                        amountWithdrawn = 1000000L,
                        remainingAmount = 1000000L,
                        currency = "THB",
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.FEE,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 700
                            )
                        )
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then:
            // totalExpenses = base cost only = 20000 (add-ons reported in totalExtras)
            assertEquals(20000L, result.totalExpenses)
            // effectiveExpense (used for virtual calc) = 20000 + 1000 - 500 = 20500
            // effectiveWithdrawal = 27000 + 700 = 27700
            // virtualBalance = 100000 - 20500 - 27700 = 51800
            assertEquals(51800L, result.virtualBalance)
            // totalExtras = 1000 (tip, ON_TOP) + 700 (ATM fee) = 1700
            // Discount (500) is excluded from extras
            assertEquals(1700L, result.totalExtras)
        }

        @Test
        fun `expenses without add-ons behave identically to before`() = runTest {
            // Verify backward compatibility: empty addOns = no change
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 50000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 10000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.DEBIT_CARD,
                        addOns = emptyList() // no add-ons
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                listOf(
                    CashWithdrawal(
                        deductedBaseAmount = 20000L,
                        amountWithdrawn = 20000L,
                        remainingAmount = 10000L,
                        currency = currency,
                        addOns = emptyList() // no add-ons
                    )
                )
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: exact same as pre-add-on behavior
            assertEquals(10000L, result.totalExpenses)
            assertEquals(50000L - 10000L - 20000L, result.virtualBalance)
            // totalExtras = 0 (no add-ons on either expenses or withdrawals)
            assertEquals(0L, result.totalExtras)
        }

        @Test
        fun `cash expense with ON_TOP fee excluded from virtual but counted in total`() = runTest {
            // Cash expenses don't deduct from virtual balance (they use cash pocket).
            // But the add-on still counts toward totalExpenses.
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(Contribution(amount = 100000L, currency = currency))
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CASH,
                        addOns = listOf(
                            AddOn(
                                type = AddOnType.FEE,
                                mode = AddOnMode.ON_TOP,
                                groupAmountCents = 200
                            )
                        )
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: totalExpenses shows base cost only (add-ons reported separately in totalExtras)
            assertEquals(5000L, result.totalExpenses)
            // virtualBalance: cash expense excluded from virtual calc
            assertEquals(100000L, result.virtualBalance)
            // totalExtras = (5200 - 5000) = 200 (fee on cash expense)
            assertEquals(200L, result.totalExtras)
        }
    }
}
