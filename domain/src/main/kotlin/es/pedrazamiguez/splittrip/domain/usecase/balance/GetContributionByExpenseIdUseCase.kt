package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetContributionByExpenseIdUseCase : UseCase {
    suspend operator fun invoke(groupId: String, expenseId: String): Contribution?
}
