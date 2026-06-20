package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of LocalGroupDataSource using Room.
 * This serves as the Single Source of Truth for Group data in the UI.
 */
class LocalGroupDataSourceImpl(private val groupDao: GroupDao) : LocalGroupDataSource {

    override fun getGroupsFlow(): Flow<List<Group>> = groupDao.getAllGroupsFlow().map { entities ->
        entities.toDomain()
    }

    override suspend fun getGroupById(groupId: String): Group? = groupDao.getGroupById(groupId)?.toDomain()

    override fun getGroupByIdFlow(groupId: String): Flow<Group?> = groupDao.getGroupByIdFlow(groupId).map { entity ->
        entity?.toDomain()
    }

    override suspend fun saveGroups(groups: List<Group>) {
        groupDao.insertGroups(groups.toEntity())
    }

    override suspend fun saveGroup(group: Group) {
        groupDao.insertGroup(group.toEntity())
    }

    override suspend fun replaceAllGroups(groups: List<Group>) {
        groupDao.replaceAllGroups(groups.toEntity())
    }

    override suspend fun deleteGroup(groupId: String) {
        groupDao.deleteGroup(groupId)
    }

    override suspend fun updateSyncStatus(groupId: String, syncStatus: SyncStatus) {
        groupDao.updateSyncStatus(groupId, syncStatus.name)
    }

    override suspend fun getPendingSyncGroupIds(): List<String> =
        groupDao.getPendingSyncGroupIds()

    override suspend fun clearAllGroups() {
        groupDao.clearAllGroups()
    }

    override suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String) {
        val groups = groupDao.getAllGroups()
        val updatedGroups = groups.filter { pendingUserId in it.memberIds }.map { entity ->
            entity.copy(
                memberIds = entity.memberIds.map { id -> if (id == pendingUserId) activeUserId else id }
            )
        }
        if (updatedGroups.isNotEmpty()) {
            groupDao.insertGroups(updatedGroups)
        }
    }
}
