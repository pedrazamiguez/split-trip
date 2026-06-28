package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory

class UpdateExpenseUseCaseImpl(
    private val strategyFactory: PersistExpenseStrategyFactory,
    private val groupRepository: GroupRepository
) : UpdateExpenseUseCase {

    override suspend operator fun invoke(
        groupId: String?,
        expense: Expense,
        pairedContributionScope: PayerType,
        pairedSubunitId: String?,
        preferredWithdrawalScope: PayerType?,
        preferredWithdrawalOwnerId: String?
    ): Result<Unit> {
        if (groupId.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Group ID cannot be null or blank"))
        }
        val group = groupRepository.getGroupById(groupId)
            ?: return Result.failure(IllegalArgumentException("Group not found with id: $groupId"))
        if (group.status == GroupStatus.ARCHIVED) {
            return Result.failure(GroupArchivedException(groupId))
        }
        val strategy = strategyFactory.create(isUpdate = true)
        return strategy.persist(
            groupId = groupId,
            expense = expense,
            pairedContributionScope = pairedContributionScope,
            pairedSubunitId = pairedSubunitId,
            preferredWithdrawalScope = preferredWithdrawalScope,
            preferredWithdrawalOwnerId = preferredWithdrawalOwnerId
        )
    }
}
