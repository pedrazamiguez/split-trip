package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark

interface LocalSyncCheckpointDataSource {
    suspend fun getWatermark(groupId: String, collectionName: SyncCollectionName): SyncWatermark?
    suspend fun saveWatermark(
        groupId: String,
        collectionName: SyncCollectionName,
        watermark: SyncWatermark,
        syncedAtClientMillis: Long
    )
    suspend fun clearCheckpoint(groupId: String, collectionName: SyncCollectionName)
    suspend fun clearGroupCheckpoints(groupId: String)
    suspend fun clearAllCheckpoints()
}
