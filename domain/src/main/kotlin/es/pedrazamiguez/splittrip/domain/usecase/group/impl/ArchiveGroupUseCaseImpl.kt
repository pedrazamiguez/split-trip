package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import java.time.LocalDateTime

class ArchiveGroupUseCaseImpl(
    private val groupRepository: GroupRepository
) : ArchiveGroupUseCase {

    override suspend operator fun invoke(groupId: String): Result<Unit> = runCatching {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        val updatedGroup = group.copy(
            status = GroupStatus.ARCHIVED,
            lastUpdatedAt = LocalDateTime.now()
        )
        groupRepository.updateGroup(updatedGroup)
    }
}
