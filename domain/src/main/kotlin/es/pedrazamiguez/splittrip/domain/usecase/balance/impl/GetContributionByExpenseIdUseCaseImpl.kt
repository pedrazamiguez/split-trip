package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase

class GetContributionByExpenseIdUseCaseImpl(
    private val contributionRepository: ContributionRepository,
    private val groupMembershipService: GroupMembershipService
) : GetContributionByExpenseIdUseCase {

    override suspend operator fun invoke(groupId: String, expenseId: String): Contribution? {
        groupMembershipService.requireMembership(groupId)
        return contributionRepository.findByLinkedExpenseId(groupId, expenseId)
    }
}
