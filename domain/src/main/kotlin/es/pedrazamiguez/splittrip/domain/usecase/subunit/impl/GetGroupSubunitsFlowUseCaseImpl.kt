package es.pedrazamiguez.splittrip.domain.usecase.subunit.impl

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetGroupSubunitsFlowUseCaseImpl(private val subunitRepository: SubunitRepository) : GetGroupSubunitsFlowUseCase {

    override operator fun invoke(groupId: String): Flow<List<Subunit>> = subunitRepository.getGroupSubunitsFlow(groupId)
}
