package es.pedrazamiguez.splittrip.domain.usecase.expense.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService.FifoCashResult
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PersistExpenseStrategy - Composite Expenses and Helpers")
class PersistExpenseStrategyTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var addOnCalculationService: AddOnCalculationService
    private lateinit var strategy: AddExpensePersistStrategy

    private val groupId = "group-1"
    private val currentUserId = "user-1"

    @BeforeEach
    fun setUp() {
        expenseRepository = mockk(relaxed = true)
        cashWithdrawalRepository = mockk(relaxed = true)
        expenseCalculatorService = mockk()
        exchangeRateCalculationService = mockk(relaxed = true)
        groupMembershipService = mockk()
        contributionRepository = mockk(relaxed = true)
        authenticationService = mockk()
        addOnCalculationService = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        every { authenticationService.requireUserId() } returns currentUserId
        every { addOnCalculationService.calculateEffectiveGroupAmount(any(), any()) } answers { firstArg() }

        strategy = AddExpensePersistStrategy(
            expenseRepository = expenseRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            expenseCalculatorService = expenseCalculatorService,
            exchangeRateCalculationService = exchangeRateCalculationService,
            groupMembershipService = groupMembershipService,
            contributionRepository = contributionRepository,
            authenticationService = authenticationService,
            addOnCalculationService = addOnCalculationService
        )
    }

    @Test
    fun `persists composite expense with cash and non-cash tranches successfully`() = runTest {
        val withdrawal = createWithdrawal("w-1", 5000L)
        coEvery {
            cashWithdrawalRepository.getAvailableWithdrawals(groupId, "EUR", PayerType.GROUP, null)
        } returns listOf(withdrawal)

        every { expenseCalculatorService.hasInsufficientCash(2000L, any()) } returns false
        every {
            expenseCalculatorService.calculateFifoCashAmount(2000L, any())
        } returns FifoCashResult(groupAmountCents = 2000L, tranches = listOf(CashTranche("w-1", 2000L)))
        every { exchangeRateCalculationService.calculateBlendedRate(2000L, 2000L) } returns BigDecimal.ONE

        coEvery { expenseRepository.addCashExpense(groupId, any(), any()) } returns true
        coEvery { cashWithdrawalRepository.updateRemainingAmounts(groupId, any()) } just Runs

        val sub1 = createSubExpense("s-1", PaymentMethod.CASH, PayerType.GROUP, null, 2000L)
        val sub2 = createSubExpense("s-2", PaymentMethod.CREDIT_CARD, PayerType.USER, currentUserId, 3000L)
        val expense = createExpense(listOf(sub1, sub2), 5000L)

        val result = strategy.persist(groupId, expense, PayerType.USER, null, null, null)

        assertTrue(result.isSuccess)
        coVerify {
            contributionRepository.addContribution(
                groupId = groupId,
                contribution = match { it.amount == 3000L && it.linkedExpenseId == "exp-1" }
            )
        }
    }

    @Test
    fun `throws InsufficientCashException when composite subExpense exceeds available cash`() = runTest {
        coEvery {
            cashWithdrawalRepository.getAvailableWithdrawals(groupId, "EUR", PayerType.GROUP, null)
        } returns listOf(createWithdrawal("w-1", 1000L))

        every { expenseCalculatorService.hasInsufficientCash(2000L, any()) } returns true

        val sub1 = createSubExpense("s-1", PaymentMethod.CASH, PayerType.GROUP, null, 2000L)
        val expense = createExpense(listOf(sub1), 2000L)

        val result = strategy.persist(groupId, expense, PayerType.USER, null, null, null)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InsufficientCashException)
    }

    @Test
    fun `uses preferred scope when provided for composite expense`() = runTest {
        val withdrawal = createWithdrawal("w-sub", 5000L)
        coEvery {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = "EUR",
                scope = PayerType.SUBUNIT,
                scopeOwnerId = "sub-1"
            )
        } returns listOf(withdrawal)

        every { expenseCalculatorService.hasInsufficientCash(2000L, any()) } returns false
        every {
            expenseCalculatorService.calculateFifoCashAmount(2000L, any())
        } returns FifoCashResult(groupAmountCents = 2000L, tranches = listOf(CashTranche("w-sub", 2000L)))
        every { exchangeRateCalculationService.calculateBlendedRate(2000L, 2000L) } returns BigDecimal.ONE

        coEvery { expenseRepository.addCashExpense(groupId, any(), any()) } returns true
        coEvery { cashWithdrawalRepository.updateRemainingAmounts(groupId, any()) } just Runs

        val sub1 = createSubExpense("s-1", PaymentMethod.CASH, PayerType.SUBUNIT, "sub-1", 2000L)
        val expense = createExpense(listOf(sub1), 2000L)

        val result = strategy.persist(
            groupId = groupId,
            expense = expense,
            pairedContributionScope = PayerType.SUBUNIT,
            pairedSubunitId = "sub-1",
            preferredWithdrawalScope = PayerType.SUBUNIT,
            preferredWithdrawalOwnerId = "sub-1"
        )

        assertTrue(result.isSuccess)
    }

    private fun createWithdrawal(id: String, remainingAmount: Long) = CashWithdrawal(
        id = id,
        groupId = groupId,
        withdrawnBy = currentUserId,
        amountWithdrawn = 5000L,
        remainingAmount = remainingAmount,
        currency = "EUR"
    )

    private fun createSubExpense(
        id: String,
        method: PaymentMethod,
        payerType: PayerType,
        payerId: String?,
        cents: Long
    ) = SubExpense(
        id = id,
        title = "Tranche",
        amountCents = cents,
        currency = "EUR",
        groupAmountCents = cents,
        exchangeRate = BigDecimal.ONE,
        paymentMethod = method,
        paymentStatus = PaymentStatus.FINISHED,
        payerType = payerType,
        payerId = payerId
    )

    private fun createExpense(subExpenses: List<SubExpense>, amount: Long) = Expense(
        id = "exp-1",
        groupId = groupId,
        title = "Tour",
        sourceAmount = amount,
        sourceCurrency = "EUR",
        groupAmount = amount,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        paymentMethod = PaymentMethod.CASH,
        paymentStatus = PaymentStatus.FINISHED,
        subExpenses = subExpenses
    )
}
