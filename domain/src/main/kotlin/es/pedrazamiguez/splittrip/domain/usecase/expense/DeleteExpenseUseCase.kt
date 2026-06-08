package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DeleteExpenseUseCase : UseCase {
    suspend operator fun invoke(groupId: String, expenseId: String)
}
