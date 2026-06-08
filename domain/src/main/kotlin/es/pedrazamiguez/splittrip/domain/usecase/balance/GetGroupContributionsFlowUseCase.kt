package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupContributionsFlowUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<List<Contribution>>
}
