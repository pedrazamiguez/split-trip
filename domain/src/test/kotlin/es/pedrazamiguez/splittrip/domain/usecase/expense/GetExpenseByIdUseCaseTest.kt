package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.GetExpenseByIdUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetExpenseByIdUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var useCase: GetExpenseByIdUseCase

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
        useCase = GetExpenseByIdUseCaseImpl(expenseRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns expense when found in repository`() = runTest {
            // Given
            coEvery { expenseRepository.getExpenseById("expense-123") } returns testExpense

            // When
            val result = useCase("expense-123")

            // Then
            assertEquals(testExpense, result)
        }

        @Test
        fun `returns null when expense not found`() = runTest {
            // Given
            coEvery { expenseRepository.getExpenseById("nonexistent-id") } returns null

            // When
            val result = useCase("nonexistent-id")

            // Then
            assertNull(result)
        }

        @Test
        fun `delegates to repository with the provided expenseId`() = runTest {
            // Given
            val expenseId = "specific-expense-789"
            coEvery { expenseRepository.getExpenseById(expenseId) } returns testExpense

            // When
            useCase(expenseId)

            // Then
            coVerify(exactly = 1) { expenseRepository.getExpenseById(expenseId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            val exception = RuntimeException("DB error")
            coEvery { expenseRepository.getExpenseById(any()) } throws exception

            // When/Then
            try {
                useCase("expense-123")
                org.junit.jupiter.api.Assertions.fail("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("DB error", e.message)
            }
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `returns expense with all fields populated`() = runTest {
            // Given
            val fullExpense = testExpense.copy(
                notes = "Birthday dinner",
                vendor = "La Bella Italia",
                paymentMethod = PaymentMethod.CASH
            )
            coEvery { expenseRepository.getExpenseById("expense-123") } returns fullExpense

            // When
            val result = useCase("expense-123")

            // Then
            assertEquals(fullExpense.notes, result?.notes)
            assertEquals(fullExpense.vendor, result?.vendor)
            assertEquals(fullExpense.paymentMethod, result?.paymentMethod)
        }

        @Test
        fun `calls repository once per invocation`() = runTest {
            // Given
            coEvery { expenseRepository.getExpenseById(any()) } returns testExpense

            // When
            useCase("expense-123")
            useCase("expense-456")

            // Then
            coVerify(exactly = 1) { expenseRepository.getExpenseById("expense-123") }
            coVerify(exactly = 1) { expenseRepository.getExpenseById("expense-456") }
        }
    }
}
