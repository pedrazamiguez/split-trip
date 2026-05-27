package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService

/**
 * Retrieves the contribution linked to a specific expense ID.
 *
 * Verifies group membership before querying the repository. Paired contributions
 * are managed automatically as side-effects of out-of-pocket expenses; this use case
 * allows loading a linked contribution's details during expense modification.
 */
class GetContributionByExpenseIdUseCase(
    private val contributionRepository: ContributionRepository,
    private val groupMembershipService: GroupMembershipService
) {
    suspend operator fun invoke(groupId: String, expenseId: String): Contribution? {
        groupMembershipService.requireMembership(groupId)
        return contributionRepository.findByLinkedExpenseId(groupId, expenseId)
    }
}
