package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.GroupExpenseConfig
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetGroupExpenseConfigUseCase : UseCase {
    suspend operator fun invoke(groupId: String?, forceRefresh: Boolean = false): Result<GroupExpenseConfig>
}
