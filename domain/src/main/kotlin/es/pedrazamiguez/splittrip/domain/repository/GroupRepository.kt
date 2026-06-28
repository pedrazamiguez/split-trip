package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    suspend fun createGroup(group: Group): String
    suspend fun getGroupById(groupId: String): Group?
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(groupId: String)
    fun getAllGroupsFlow(): Flow<List<Group>>
    suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String)
}
