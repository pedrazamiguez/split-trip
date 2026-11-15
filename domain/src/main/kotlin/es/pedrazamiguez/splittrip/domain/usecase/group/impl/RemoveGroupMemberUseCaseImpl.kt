package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetNonRemovableGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase

class RemoveGroupMemberUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val getNonRemovableGroupMembersUseCase: GetNonRemovableGroupMembersUseCase
) : RemoveGroupMemberUseCase {

    override suspend operator fun invoke(groupId: String, userId: String): Result<Unit> = runCatching {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found: $groupId")

        if (group.status == GroupStatus.ARCHIVED) throw GroupArchivedException(groupId)
        if (userId !in group.members) throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.NOT_A_MEMBER)

        val nonRemovableReason = getNonRemovableGroupMembersUseCase(groupId)[userId]
        if (nonRemovableReason != null) {
            throw CannotRemoveMemberException(nonRemovableReason)
        }

        groupRepository.removeMember(groupId, userId)
    }
}
