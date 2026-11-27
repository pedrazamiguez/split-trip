package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.GroupSyncCheckpointEntity

@Dao
interface GroupSyncCheckpointDao {

    @Query(
        "SELECT * FROM group_sync_checkpoints " +
            "WHERE groupId = :groupId AND collectionName = :collectionName LIMIT 1"
    )
    suspend fun getCheckpoint(groupId: String, collectionName: String): GroupSyncCheckpointEntity?

    @Upsert
    suspend fun upsertCheckpoint(checkpoint: GroupSyncCheckpointEntity)

    @Query(
        "DELETE FROM group_sync_checkpoints " +
            "WHERE groupId = :groupId AND collectionName = :collectionName"
    )
    suspend fun deleteCheckpoint(groupId: String, collectionName: String)

    @Query("DELETE FROM group_sync_checkpoints WHERE groupId = :groupId")
    suspend fun deleteGroupCheckpoints(groupId: String)

    @Query("DELETE FROM group_sync_checkpoints")
    suspend fun clearAllCheckpoints()
}
