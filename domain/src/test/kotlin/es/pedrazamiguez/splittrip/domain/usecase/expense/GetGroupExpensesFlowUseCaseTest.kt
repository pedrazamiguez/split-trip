package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetGroupExpensesFlowUseCaseTest {

    private val repository: ExpenseRepository = mockk()
    private val useCase = GetGroupExpensesFlowUseCase(repository)

    @Test
    fun `invoke delegates to repository`() {
        val groupId = "group_123"
        val expectedFlow = flowOf(emptyList<Expense>())
        every { repository.getGroupExpensesFlow(groupId) } returns expectedFlow

        val resultFlow = useCase(groupId)

        assertEquals(expectedFlow, resultFlow)
        verify(exactly = 1) { repository.getGroupExpensesFlow(groupId) }
    }
}
