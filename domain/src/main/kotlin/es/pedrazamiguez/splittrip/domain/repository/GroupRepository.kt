package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(group: Group): String
    suspend fun getGroupById(groupId: String): Group?
    fun getGroupByIdFlow(groupId: String): Flow<Group?>
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
    fun getAllGroupsFlow(): Flow<List<Group>>
    suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String)

    /**
     * Removes the current user from the group.
     *
     * Offline-first: saves the updated group (with the user removed from members) to
     * Room immediately, then syncs to Firestore in the background.
     *
     * @param groupId The ID of the group to leave.
     */
    suspend fun leaveGroup(groupId: String)

    /**
     * Adds new members to an existing group.
     *
     * Offline-first: saves the updated group (with new members appended) to
     * Room immediately, then syncs to Firestore in the background.
     *
     * @param groupId The ID of the group to add members to.
     * @param newMemberIds The IDs of the new members to add.
     */
    suspend fun addMembers(groupId: String, newMemberIds: List<String>)

    /**
     * Removes a member from a group.
     *
     * Offline-first: saves the updated group (with the user removed from members) to
     * Room immediately, then syncs to Firestore in the background.
     *
     * @param groupId The ID of the group to remove the member from.
     * @param userId The ID of the user to remove.
     */
    suspend fun removeMember(groupId: String, userId: String)
}
