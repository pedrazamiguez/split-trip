package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdLocalUseCase

class GetGroupByIdLocalUseCaseImpl(
    private val groupRepository: GroupRepository
) : GetGroupByIdLocalUseCase {
    override suspend operator fun invoke(groupId: String): Group? =
        groupRepository.getGroupByIdLocal(groupId)
}
