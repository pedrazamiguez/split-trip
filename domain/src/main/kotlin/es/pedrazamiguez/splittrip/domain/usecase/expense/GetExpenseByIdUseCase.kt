package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetExpenseByIdUseCase : UseCase {
    suspend operator fun invoke(expenseId: String): Expense?
}
