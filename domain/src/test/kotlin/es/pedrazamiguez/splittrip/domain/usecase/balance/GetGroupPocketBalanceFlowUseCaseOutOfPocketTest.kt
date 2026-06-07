package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupPocketBalanceFlowUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetGroupPocketBalanceFlowUseCase — Out-of-Pocket Scenarios")
class GetGroupPocketBalanceFlowUseCaseOutOfPocketTest {

    private lateinit var contributionRepository: ContributionRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var useCase: GetGroupPocketBalanceFlowUseCase

    private val groupId = "group-oop"
    private val currency = "EUR"

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
    @DisplayName("Out-of-pocket expense with paired contribution")
    inner class OutOfPocketWithPairedContribution {

        @Test
        fun `paired contribution and non-cash expense cancel out in virtual balance`() = runTest {
            // Given: out-of-pocket expense 50 EUR + paired contribution 50 EUR
            val pairedContribution = Contribution(
                amount = 5000L,
                currency = currency,
                linkedExpenseId = "exp-oop-1",
                contributionScope = PayerType.USER,
                userId = "maria-001"
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(pairedContribution)
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        id = "exp-oop-1",
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        payerType = PayerType.USER,
                        payerId = "maria-001"
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = contribution(5000) - expense(5000) - withdrawals(0) = 0
            assertEquals(0L, result.virtualBalance)
            assertEquals(5000L, result.totalContributions)
            assertEquals(5000L, result.totalExpenses)
        }

        @Test
        fun `out-of-pocket with add-ons uses effective amount`() = runTest {
            // Given: out-of-pocket expense with 5 EUR tip ON_TOP
            // base = 50 EUR, effective = 55 EUR, paired contribution = 55 EUR
            val tipAddOn = AddOn(
                type = AddOnType.TIP,
                mode = AddOnMode.ON_TOP,
                groupAmountCents = 500L
            )
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(
                    Contribution(
                        amount = 5500L,
                        currency = currency,
                        linkedExpenseId = "exp-oop-tip",
                        contributionScope = PayerType.USER,
                        userId = "maria-001"
                    )
                )
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        id = "exp-oop-tip",
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        payerType = PayerType.USER,
                        payerId = "maria-001",
                        addOns = listOf(tipAddOn)
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then: virtualBalance = 5500 - 5500(effective) - 0 = 0
            assertEquals(0L, result.virtualBalance)
            assertEquals(5500L, result.totalContributions)
        }

        @Test
        fun `mixed group-funded and out-of-pocket expenses compute correctly`() = runTest {
            // Given:
            // - Manual contribution: 200 EUR
            // - Paired contribution (OOP): 50 EUR
            // - Group expense (card): 80 EUR
            // - OOP expense (card): 50 EUR
            every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(
                listOf(
                    Contribution(amount = 20000L, currency = currency),
                    Contribution(
                        amount = 5000L,
                        currency = currency,
                        linkedExpenseId = "exp-oop",
                        contributionScope = PayerType.USER
                    )
                )
            )
            every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(
                listOf(
                    Expense(
                        id = "exp-group",
                        groupAmount = 8000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        payerType = PayerType.GROUP
                    ),
                    Expense(
                        id = "exp-oop",
                        groupAmount = 5000L,
                        groupCurrency = currency,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        payerType = PayerType.USER,
                        payerId = "maria-001"
                    )
                )
            )
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(
                emptyList()
            )

            // When
            val result = useCase(groupId, currency).first()

            // Then:
            // totalContributions = 20000 + 5000 = 25000
            // virtualExpenses = 8000 + 5000 = 13000 (both non-cash)
            // virtualBalance = 25000 - 13000 - 0 = 12000
            assertEquals(25000L, result.totalContributions)
            assertEquals(12000L, result.virtualBalance)
        }
    }
}
