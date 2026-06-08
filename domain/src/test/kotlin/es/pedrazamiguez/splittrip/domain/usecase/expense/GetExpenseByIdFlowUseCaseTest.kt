package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetExpenseByIdFlowUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetExpenseByIdFlowUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var useCase: GetExpenseByIdFlowUseCase

    private val testExpense = Expense(
        id = "expense-123",
        groupId = "group-456",
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD
    )

    @BeforeEach
    fun setUp() {
        expenseRepository = mockk()
        useCase = GetExpenseByIdFlowUseCaseImpl(expenseRepository)
    }

    @Test
    fun `invoke returns flow of expense from repository`() = runTest {
        val flow = flowOf(testExpense)
        every { expenseRepository.getExpenseByIdFlow("expense-123") } returns flow

        val result = useCase("expense-123").toList()

        assertEquals(1, result.size)
        assertEquals(testExpense, result[0])
        verify(exactly = 1) { expenseRepository.getExpenseByIdFlow("expense-123") }
    }
}
