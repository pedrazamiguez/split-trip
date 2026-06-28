package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase

class DeleteContributionUseCaseImpl(
    private val contributionRepository: ContributionRepository,
    private val groupMembershipService: GroupMembershipService,
    private val groupRepository: GroupRepository
) : DeleteContributionUseCase {

    override suspend operator fun invoke(groupId: String, contributionId: String) {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        if (group.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(groupId)
        }
        groupMembershipService.requireMembership(groupId)
        contributionRepository.deleteContribution(groupId, contributionId)
    }
}
