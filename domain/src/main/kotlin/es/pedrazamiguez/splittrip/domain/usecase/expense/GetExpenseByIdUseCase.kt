package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository

class GetExpenseByIdUseCase(private val expenseRepository: ExpenseRepository) {
    suspend operator fun invoke(expenseId: String): Expense? =
        expenseRepository.getExpenseById(expenseId)
}
