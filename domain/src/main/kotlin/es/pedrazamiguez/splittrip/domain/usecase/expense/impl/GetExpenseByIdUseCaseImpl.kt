package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase

class GetExpenseByIdUseCaseImpl(private val expenseRepository: ExpenseRepository) : GetExpenseByIdUseCase {

    override suspend operator fun invoke(expenseId: String): Expense? =
        expenseRepository.getExpenseById(expenseId)
}
