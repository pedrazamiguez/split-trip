package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupExpensesFlowUseCaseImpl(private val expenseRepository: ExpenseRepository) : GetGroupExpensesFlowUseCase {

    override operator fun invoke(groupId: String): Flow<List<Expense>> = expenseRepository.getGroupExpensesFlow(groupId)
}
