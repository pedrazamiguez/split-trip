package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetExpenseByIdFlowUseCase(private val expenseRepository: ExpenseRepository) {
    operator fun invoke(expenseId: String): Flow<Expense?> =
        expenseRepository.getExpenseByIdFlow(expenseId)
}
