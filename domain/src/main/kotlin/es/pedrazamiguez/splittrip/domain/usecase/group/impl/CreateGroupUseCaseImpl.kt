package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase

class CreateGroupUseCaseImpl(private val groupRepository: GroupRepository) : CreateGroupUseCase {

    override suspend operator fun invoke(group: Group): Result<String> = runCatching {
        groupRepository.createGroup(group)
    }
}
