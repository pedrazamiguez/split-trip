package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase

class UpdateGroupUseCaseImpl(
    private val groupRepository: GroupRepository
) : UpdateGroupUseCase {

    override suspend operator fun invoke(group: Group): Result<Unit> = runCatching {
        val existingGroup = groupRepository.getGroupById(group.id)
            ?: throw IllegalArgumentException("Group not found with id: ${group.id}")
        if (existingGroup.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(group.id)
        }
        groupRepository.updateGroup(group)
    }
}
