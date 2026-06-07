package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetExpenseByIdFlowUseCaseImpl(private val expenseRepository: ExpenseRepository) : GetExpenseByIdFlowUseCase {

    override operator fun invoke(expenseId: String): Flow<Expense?> =
        expenseRepository.getExpenseByIdFlow(expenseId)
}
