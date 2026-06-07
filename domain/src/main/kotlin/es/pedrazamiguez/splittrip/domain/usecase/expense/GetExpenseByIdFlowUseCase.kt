package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetExpenseByIdFlowUseCase : UseCase {
    operator fun invoke(expenseId: String): Flow<Expense?>
}
