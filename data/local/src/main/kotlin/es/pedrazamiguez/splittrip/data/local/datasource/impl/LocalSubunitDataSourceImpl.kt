package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.SubunitDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSubunitDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalSubunitDataSourceImpl(private val subunitDao: SubunitDao) : LocalSubunitDataSource {

    override fun getSubunitsByGroupIdFlow(groupId: String): Flow<List<Subunit>> =
        subunitDao.getSubunitsByGroupIdFlow(groupId).map { entities ->
            entities.toDomain()
        }

    override suspend fun getSubunitsByGroupId(groupId: String): List<Subunit> =
        subunitDao.getSubunitsByGroupId(groupId).toDomain()

    override suspend fun saveSubunit(subunit: Subunit) {
        subunitDao.insertSubunit(subunit.toEntity())
    }

    override suspend fun deleteSubunit(subunitId: String) {
        subunitDao.deleteSubunit(subunitId)
    }

    override suspend fun deleteSubunitsByGroupId(groupId: String) {
        subunitDao.deleteSubunitsByGroupId(groupId)
    }

    override suspend fun replaceSubunitsForGroup(groupId: String, subunits: List<Subunit>) {
        subunitDao.replaceSubunitsForGroup(
            groupId,
            subunits.map { it.toEntity() }
        )
    }

    override suspend fun applySubunitsDeltaForGroup(
        groupId: String,
        upserts: List<Subunit>,
        deletedIds: List<String>
    ) {
        subunitDao.applySubunitsDeltaForGroup(
            groupId = groupId,
            upserts = upserts.map { it.toEntity() },
            deletedIds = deletedIds
        )
    }

    override suspend fun getSubunitIdsByGroup(groupId: String): List<String> =
        subunitDao.getSubunitIdsByGroupId(groupId)

    override suspend fun getSubunitById(subunitId: String): Subunit? = subunitDao.getSubunitById(subunitId)?.toDomain()

    override suspend fun updateSyncStatus(subunitId: String, syncStatus: SyncStatus) {
        subunitDao.updateSyncStatus(subunitId, syncStatus.name)
    }

    override suspend fun getPendingSyncSubunitIds(groupId: String): List<String> =
        subunitDao.getPendingSyncSubunitIds(groupId)

    override suspend fun clearAllSubunits() {
        subunitDao.clearAllSubunits()
    }
}
