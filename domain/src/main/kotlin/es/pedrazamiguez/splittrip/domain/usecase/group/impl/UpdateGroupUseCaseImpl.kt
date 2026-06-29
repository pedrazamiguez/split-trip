package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase

class UpdateGroupUseCaseImpl(
    private val groupRepository: GroupRepository
) : UpdateGroupUseCase {

    override suspend operator fun invoke(group: Group): Result<Unit> = runCatching {
        groupRepository.updateGroup(group)
    }
}
