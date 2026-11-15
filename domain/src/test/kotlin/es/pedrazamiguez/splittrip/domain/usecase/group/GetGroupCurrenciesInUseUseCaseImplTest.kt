package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetGroupCurrenciesInUseUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetGroupCurrenciesInUseUseCaseImplTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var cashTransferRepository: CashTransferRepository
    private lateinit var useCase: GetGroupCurrenciesInUseUseCase

    private val groupId = "group-123"

    @BeforeEach
    fun setUp() {
        expenseRepository = mockk()
        contributionRepository = mockk()
        cashWithdrawalRepository = mockk()
        cashTransferRepository = mockk()

        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(emptyList())

        useCase = GetGroupCurrenciesInUseUseCaseImpl(
            expenseRepository = expenseRepository,
            contributionRepository = contributionRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            cashTransferRepository = cashTransferRepository
        )
    }

    @Test
    fun `returns empty set when group has no transactions`() = runTest {
        val result = useCase(groupId)

        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `collects sourceCurrency, groupCurrency, and addOn currencies from expenses`() = runTest {
        val expense = Expense(
            id = "exp-1",
            groupId = groupId,
            sourceCurrency = "JPY",
            groupCurrency = "EUR",
            addOns = listOf(AddOn(id = "ao-1", currency = "USD"))
        )
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(listOf(expense))

        val result = useCase(groupId)

        assertEquals(setOf("JPY", "EUR", "USD"), result)
    }

    @Test
    fun `collects currencies from contributions`() = runTest {
        val contribution = Contribution(id = "contrib-1", groupId = groupId, currency = "GBP")
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(listOf(contribution))

        val result = useCase(groupId)

        assertTrue("GBP" in result)
    }

    @Test
    fun `collects currency and addOn currencies from cash withdrawals`() = runTest {
        val withdrawal = CashWithdrawal(
            id = "wd-1",
            groupId = groupId,
            currency = "THB",
            addOns = listOf(AddOn(id = "ao-2", currency = "EUR"))
        )
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(listOf(withdrawal))

        val result = useCase(groupId)

        assertEquals(setOf("THB", "EUR"), result)
    }

    @Test
    fun `collects currencies from cash transfers`() = runTest {
        val transfer = CashTransfer(
            id = "tx-1",
            groupId = groupId,
            fromUserId = "u1",
            toUserId = "u2",
            amountCents = 1000L,
            currency = "MXN",
            equivalentBaseAmountCents = 500L,
            createdAt = 1L
        )
        every { cashTransferRepository.observeGroupCashTransfers(groupId) } returns flowOf(listOf(transfer))

        val result = useCase(groupId)

        assertTrue("MXN" in result)
    }

    @Test
    fun `ignores blank currency codes`() = runTest {
        val expense = Expense(
            id = "exp-blank",
            groupId = groupId,
            sourceCurrency = "",
            groupCurrency = "   ",
            addOns = listOf(AddOn(id = "ao-blank", currency = ""))
        )
        val contribution = Contribution(id = "c-blank", groupId = groupId, currency = " ")
        every { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(listOf(expense))
        every { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(listOf(contribution))

        val result = useCase(groupId)

        assertFalse("" in result)
        assertFalse("   " in result)
        assertEquals(emptySet<String>(), result)
    }
}
