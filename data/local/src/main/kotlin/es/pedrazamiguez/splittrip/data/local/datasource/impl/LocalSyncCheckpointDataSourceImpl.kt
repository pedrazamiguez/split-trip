package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.GroupSyncCheckpointDao
import es.pedrazamiguez.splittrip.data.local.entity.GroupSyncCheckpointEntity
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark

class LocalSyncCheckpointDataSourceImpl(
    private val groupSyncCheckpointDao: GroupSyncCheckpointDao
) : LocalSyncCheckpointDataSource {

    override suspend fun getWatermark(
        groupId: String,
        collectionName: SyncCollectionName
    ): SyncWatermark? =
        groupSyncCheckpointDao.getCheckpoint(groupId, collectionName.collectionPath)?.let { entity ->
            SyncWatermark(
                seconds = entity.lastServerUpdatedSeconds,
                nanoseconds = entity.lastServerUpdatedNanos
            )
        }

    override suspend fun saveWatermark(
        groupId: String,
        collectionName: SyncCollectionName,
        watermark: SyncWatermark,
        syncedAtClientMillis: Long
    ) {
        groupSyncCheckpointDao.upsertCheckpoint(
            GroupSyncCheckpointEntity(
                groupId = groupId,
                collectionName = collectionName.collectionPath,
                lastServerUpdatedSeconds = watermark.seconds,
                lastServerUpdatedNanos = watermark.nanoseconds,
                lastSyncedAtClientMillis = syncedAtClientMillis
            )
        )
    }

    override suspend fun clearCheckpoint(groupId: String, collectionName: SyncCollectionName) {
        groupSyncCheckpointDao.deleteCheckpoint(groupId, collectionName.collectionPath)
    }

    override suspend fun clearGroupCheckpoints(groupId: String) {
        groupSyncCheckpointDao.deleteGroupCheckpoints(groupId)
    }

    override suspend fun clearAllCheckpoints() {
        groupSyncCheckpointDao.clearAllCheckpoints()
    }
}
