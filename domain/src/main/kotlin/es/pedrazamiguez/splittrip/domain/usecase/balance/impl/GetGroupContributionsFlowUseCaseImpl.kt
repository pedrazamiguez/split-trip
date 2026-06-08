package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupContributionsFlowUseCaseImpl(
    private val contributionRepository: ContributionRepository
) : GetGroupContributionsFlowUseCase {

    override operator fun invoke(groupId: String): Flow<List<Contribution>> =
        contributionRepository.getGroupContributionsFlow(groupId)
}
