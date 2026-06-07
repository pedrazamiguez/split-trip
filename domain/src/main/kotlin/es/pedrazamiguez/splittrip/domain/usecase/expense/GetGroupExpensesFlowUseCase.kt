package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupExpensesFlowUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<List<Expense>>
}
