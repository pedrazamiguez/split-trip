package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase

class DeleteCashWithdrawalUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val groupMembershipService: GroupMembershipService,
    private val groupRepository: GroupRepository
) : DeleteCashWithdrawalUseCase {

    override suspend operator fun invoke(groupId: String, withdrawalId: String) {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        if (group.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(groupId)
        }
        groupMembershipService.requireMembership(groupId)
        cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId)
    }
}
