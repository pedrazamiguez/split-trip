package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import es.pedrazamiguez.splittrip.data.local.entity.CashTransferEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CashTransferDao {
    @Query("SELECT * FROM cash_transfers WHERE groupId = :groupId")
    fun observeByGroupId(groupId: String): Flow<List<CashTransferEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transfer: CashTransferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<CashTransferEntity>)

    @Query("DELETE FROM cash_transfers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cash_transfers WHERE groupId = :groupId AND id NOT IN (:retainedIds)")
    suspend fun deleteStaleForGroup(groupId: String, retainedIds: List<String>)

    @Query("DELETE FROM cash_transfers WHERE groupId = :groupId AND id IN (:ids)")
    suspend fun deleteByGroupAndIds(groupId: String, ids: List<String>)

    @Query("UPDATE cash_transfers SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT id FROM cash_transfers WHERE groupId = :groupId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncCashTransferIds(groupId: String): List<String>

    @Query("SELECT id, syncStatus FROM cash_transfers WHERE groupId = :groupId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedCashTransferStatuses(groupId: String): List<SyncStatusEntry>

    @Query("SELECT id FROM cash_transfers WHERE groupId = :groupId")
    suspend fun getCashTransferIdsByGroupId(groupId: String): List<String>

    @Query("SELECT * FROM cash_transfers WHERE id = :id")
    suspend fun getCashTransferById(id: String): CashTransferEntity?

    @Query("DELETE FROM cash_transfers WHERE groupId = :groupId")
    suspend fun deleteByGroupId(groupId: String)

    @Transaction
    suspend fun replaceCashTransfersForGroup(groupId: String, remoteTransfers: List<CashTransferEntity>) {
        val unsyncedStatuses = getUnsyncedCashTransferStatuses(groupId)
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()
        val remoteIds = remoteTransfers.map { it.id }.toSet()
        val localIds = getCashTransferIdsByGroupId(groupId)

        if (remoteTransfers.isNotEmpty()) {
            insertAll(remoteTransfers)
        }
        unsyncedStatuses.forEach { entry ->
            updateSyncStatus(entry.id, entry.syncStatus)
        }

        val staleIds = localIds.filter { it !in remoteIds && it !in unsyncedIds }
        staleIds.chunked(SQLITE_BIND_LIMIT).forEach { chunk ->
            deleteByGroupAndIds(groupId, chunk)
        }
    }

    @Transaction
    suspend fun applyCashTransfersDeltaForGroup(
        groupId: String,
        upserts: List<CashTransferEntity>,
        deletedIds: List<String>
    ) {
        if (upserts.isEmpty() && deletedIds.isEmpty()) return
        val deletedIdSet = deletedIds.toSet()
        if (upserts.isNotEmpty()) {
            val unsyncedStatuses = getUnsyncedCashTransferStatuses(groupId)
            insertAll(upserts)
            val upsertedIds = upserts.map { it.id }.toSet()
            unsyncedStatuses
                .filter { it.id in upsertedIds && it.id !in deletedIdSet }
                .forEach { updateSyncStatus(it.id, it.syncStatus) }
        }
        deletedIds.chunked(SQLITE_BIND_LIMIT).forEach { chunk ->
            deleteByGroupAndIds(groupId, chunk)
        }
    }

    private companion object {
        const val SQLITE_BIND_LIMIT = 500
    }
}
