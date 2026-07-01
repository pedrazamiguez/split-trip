package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase

class DeleteGroupUseCaseImpl(private val groupRepository: GroupRepository) : DeleteGroupUseCase {

    /**
     * Deletes a group by its ID.
     *
     * The repository deletes the group from the local database immediately
     * (Room FK CASCADE handles child entities), then signals Firestore to
     * initiate a server-side cascading delete via the `onGroupDeletionRequested`
     * Cloud Function.
     *
     * @param groupId The ID of the group to delete.
     */
    override suspend operator fun invoke(groupId: String) {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found with id: $groupId")
        if (group.status == GroupStatus.ARCHIVED) {
            throw GroupArchivedException(groupId)
        }
        groupRepository.deleteGroup(groupId)
    }
}
