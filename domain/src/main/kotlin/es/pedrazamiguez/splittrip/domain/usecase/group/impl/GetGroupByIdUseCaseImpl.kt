package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase

class GetGroupByIdUseCaseImpl(private val groupRepository: GroupRepository) : GetGroupByIdUseCase {

    override suspend operator fun invoke(groupId: String): Group? = groupRepository.getGroupById(groupId)
}
