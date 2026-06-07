package es.pedrazamiguez.splittrip.domain.usecase.subunit.impl

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase

class GetGroupSubunitsUseCaseImpl(private val subunitRepository: SubunitRepository) : GetGroupSubunitsUseCase {

    override suspend operator fun invoke(groupId: String): List<Subunit> = subunitRepository.getGroupSubunits(groupId)
}
