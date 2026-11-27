package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.SettlementRecordEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementRecordDao {

    @Query("SELECT * FROM settlement_records WHERE groupId = :groupId")
    fun getByGroupIdFlow(groupId: String): Flow<List<SettlementRecordEntity>>

    @Query("SELECT * FROM settlement_records WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: String): List<SettlementRecordEntity>

    @Query(
        "SELECT * FROM settlement_records WHERE groupId = :groupId " +
            "AND (fromUserId = :userId OR toUserId = :userId)"
    )
    suspend fun getByMember(groupId: String, userId: String): List<SettlementRecordEntity>

    @Query("SELECT * FROM settlement_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SettlementRecordEntity?

    @Upsert
    suspend fun upsert(entity: SettlementRecordEntity)

    @Query("DELETE FROM settlement_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE settlement_records SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: String, syncStatus: String)

    @Query("SELECT syncStatus FROM settlement_records WHERE id = :id LIMIT 1")
    suspend fun getSyncStatus(id: String): String?

    @Query(
        "SELECT id FROM settlement_records WHERE groupId = :groupId " +
            "AND syncStatus = 'PENDING_SYNC'"
    )
    suspend fun getPendingSyncIds(groupId: String): List<String>

    @Query("DELETE FROM settlement_records WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Query("SELECT id, syncStatus FROM settlement_records WHERE groupId = :groupId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedSettlementStatuses(groupId: String): List<SyncStatusEntry>

    @Query("SELECT id FROM settlement_records WHERE groupId = :groupId")
    suspend fun getSettlementIdsByGroupId(groupId: String): List<String>

    @Query("DELETE FROM settlement_records WHERE id IN (:ids)")
    suspend fun deleteSettlementsByIds(ids: List<String>)

    @Query("SELECT * FROM settlement_records WHERE groupId = :groupId AND id IN (:ids)")
    suspend fun getByGroupAndIds(groupId: String, ids: List<String>): List<SettlementRecordEntity>

    @Query("DELETE FROM settlement_records WHERE groupId = :groupId AND id IN (:ids)")
    suspend fun deleteByGroupAndIds(groupId: String, ids: List<String>)

    @Transaction
    suspend fun replaceForGroup(groupId: String, entities: List<SettlementRecordEntity>) {
        val unsyncedStatuses = getUnsyncedSettlementStatuses(groupId)
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()

        val localEntities = getByGroupId(groupId).associateBy { it.id }
        val remoteIds = entities.map { it.id }.toSet()

        val mergedEntities = entities.map { remote ->
            mergeRemoteWithLocal(remote, localEntities[remote.id])
        }

        mergedEntities.forEach { upsert(it) }

        val staleIds = localEntities.keys.filter { it !in remoteIds && it !in unsyncedIds }
        if (staleIds.isNotEmpty()) {
            staleIds.chunked(SQLITE_BIND_LIMIT).forEach { chunk ->
                deleteSettlementsByIds(chunk)
            }
        }
    }

    @Transaction
    suspend fun applyDeltaForGroup(
        groupId: String,
        upserts: List<SettlementRecordEntity>,
        deletedIds: List<String>
    ) {
        if (upserts.isEmpty() && deletedIds.isEmpty()) return

        if (upserts.isNotEmpty()) {
            val existingById = upserts
                .map { it.id }
                .chunked(SQLITE_BIND_LIMIT)
                .flatMap { chunk -> getByGroupAndIds(groupId, chunk) }
                .associateBy { it.id }
            upserts
                .map { remote -> mergeRemoteWithLocal(remote, existingById[remote.id]) }
                .forEach { upsert(it) }
        }

        deletedIds.chunked(SQLITE_BIND_LIMIT).forEach { chunk ->
            deleteByGroupAndIds(groupId, chunk)
        }
    }

    private fun mergeRemoteWithLocal(
        remote: SettlementRecordEntity,
        local: SettlementRecordEntity?
    ): SettlementRecordEntity {
        if (local == null) return remote
        // If local is not synced, preserve local modifications but take remote fields
        // that aren't user-editable (e.g. metadata)
        return if (local.syncStatus != "SYNCED") {
            remote.copy(
                status = local.status,
                confirmedByPayerAt = local.confirmedByPayerAt,
                confirmedByPayeeAt = local.confirmedByPayeeAt,
                resolvedAt = local.resolvedAt,
                disputedBy = local.disputedBy,
                disputeReason = local.disputeReason,
                syncStatus = local.syncStatus
            )
        } else {
            remote.copy(syncStatus = "SYNCED")
        }
    }

    private companion object {
        const val SQLITE_BIND_LIMIT = 500
    }
}
