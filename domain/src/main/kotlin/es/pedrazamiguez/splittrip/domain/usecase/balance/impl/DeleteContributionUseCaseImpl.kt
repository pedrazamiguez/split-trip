package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase

class DeleteContributionUseCaseImpl(
    private val contributionRepository: ContributionRepository,
    private val groupMembershipService: GroupMembershipService
) : DeleteContributionUseCase {

    override suspend operator fun invoke(groupId: String, contributionId: String) {
        groupMembershipService.requireMembership(groupId)
        contributionRepository.deleteContribution(groupId, contributionId)
    }
}
