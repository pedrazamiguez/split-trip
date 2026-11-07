package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.CashWithdrawalEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cash withdrawal records.
 *
 * The function count naturally exceeds Detekt's `TooManyFunctions` threshold because
 * a single entity DAO accumulates distinct query methods: inserts, lookups, FIFO
 * availability queries, remaining-amount updates, deletions, sync-status reads/writes,
 * and the @Transaction reconciliation helper. All methods are cohesive (they all
 * operate on the `cash_withdrawals` table) and splitting into read/write DAOs would
 * add architectural complexity with no real ISP benefit.
 */
@Suppress("TooManyFunctions")
@Dao
interface CashWithdrawalDao {

    @Upsert
    suspend fun insertWithdrawal(withdrawal: CashWithdrawalEntity)

    @Upsert
    suspend fun insertWithdrawals(withdrawals: List<CashWithdrawalEntity>)

    @Query("SELECT * FROM cash_withdrawals WHERE groupId = :groupId ORDER BY createdAtMillis DESC")
    fun getWithdrawalsByGroupIdFlow(groupId: String): Flow<List<CashWithdrawalEntity>>

    @Query("SELECT * FROM cash_withdrawals WHERE id = :withdrawalId")
    suspend fun getWithdrawalById(withdrawalId: String): CashWithdrawalEntity?

    @Query("SELECT * FROM cash_withdrawals WHERE id IN (:withdrawalIds)")
    suspend fun getWithdrawalsByIds(withdrawalIds: List<String>): List<CashWithdrawalEntity>

    /**
     * Fetches available (non-exhausted) withdrawals for FIFO consumption.
     * Ordered by createdAt ascending (oldest first) for FIFO ordering.
     *
     * **Scope-blind:** returns all withdrawals regardless of [withdrawalScope].
     * Kept for reconciliation/test purposes only. Use the scoped variants below
     * for FIFO pool selection in expense processing.
     */
    @Query(
        """
        SELECT * FROM cash_withdrawals
        WHERE groupId = :groupId AND currency = :currency AND remainingAmount > 0
        ORDER BY createdAtMillis ASC
        """
    )
    suspend fun getAvailableByGroupAndCurrency(groupId: String, currency: String): List<CashWithdrawalEntity>

    /**
     * Fetches available GROUP-scoped withdrawals for FIFO consumption.
     * Used as the primary pool for GROUP expenses and as the fallback pool
     * for USER and SUBUNIT expenses when their own pool is insufficient.
     */
    @Query(
        """
        SELECT * FROM cash_withdrawals
        WHERE groupId = :groupId AND currency = :currency
              AND withdrawalScope = 'GROUP' AND remainingAmount > 0
        ORDER BY createdAtMillis ASC
        """
    )
    suspend fun getAvailableGroupScopedByGroupAndCurrency(
        groupId: String,
        currency: String
    ): List<CashWithdrawalEntity>

    /**
     * Fetches available USER-scoped withdrawals for a specific user, for FIFO consumption.
     * Used as the primary pool for USER (personal) cash expenses.
     */
    @Query(
        """
        SELECT * FROM cash_withdrawals
        WHERE groupId = :groupId AND currency = :currency
              AND withdrawalScope = 'USER' AND withdrawnBy = :withdrawnBy AND remainingAmount > 0
        ORDER BY createdAtMillis ASC
        """
    )
    suspend fun getAvailableUserScopedByGroupAndCurrency(
        groupId: String,
        currency: String,
        withdrawnBy: String
    ): List<CashWithdrawalEntity>

    /**
     * Fetches available SUBUNIT-scoped withdrawals for a specific subunit, for FIFO consumption.
     * Used as the primary pool for SUBUNIT cash expenses.
     */
    @Query(
        """
        SELECT * FROM cash_withdrawals
        WHERE groupId = :groupId AND currency = :currency
              AND withdrawalScope = 'SUBUNIT' AND subunitId = :subunitId AND remainingAmount > 0
        ORDER BY createdAtMillis ASC
        """
    )
    suspend fun getAvailableSubunitScopedByGroupAndCurrency(
        groupId: String,
        currency: String,
        subunitId: String
    ): List<CashWithdrawalEntity>

    @Query("UPDATE cash_withdrawals SET remainingAmount = :newRemaining WHERE id = :withdrawalId")
    suspend fun updateRemainingAmount(withdrawalId: String, newRemaining: Long)

    /**
     * Atomically updates the remaining amount on multiple withdrawals in a single transaction.
     * Used during FIFO cash expense processing to batch all tranche deductions together.
     */
    @Transaction
    suspend fun updateRemainingAmounts(updates: List<Pair<String, Long>>) {
        for ((withdrawalId, newRemaining) in updates) {
            updateRemainingAmount(withdrawalId, newRemaining)
        }
    }

    @Query("DELETE FROM cash_withdrawals WHERE id = :withdrawalId")
    suspend fun deleteWithdrawal(withdrawalId: String)

    /**
     * Updates the sync status of a single cash withdrawal.
     * Used to transition between PENDING_SYNC → SYNCED or SYNC_FAILED after cloud sync.
     */
    @Query("UPDATE cash_withdrawals SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM cash_withdrawals WHERE groupId = :groupId")
    suspend fun deleteWithdrawalsByGroupId(groupId: String)

    @Query("SELECT id FROM cash_withdrawals WHERE groupId = :groupId")
    suspend fun getWithdrawalIdsByGroupId(groupId: String): List<String>

    @Query("DELETE FROM cash_withdrawals")
    suspend fun clearAllWithdrawals()

    /**
     * Returns sync status metadata for withdrawals in a group that are NOT fully synced.
     * Used during cloud snapshot reconciliation to preserve PENDING_SYNC / SYNC_FAILED
     * statuses that would otherwise be overwritten by the upsert (which defaults to SYNCED).
     */
    @Query("SELECT id, syncStatus FROM cash_withdrawals WHERE groupId = :groupId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedWithdrawalStatuses(groupId: String): List<SyncStatusEntry>

    /**
     * Returns IDs of withdrawals in a group that are still waiting for server confirmation.
     * Used after reconciliation to attempt server verification and transition to SYNCED.
     */
    @Query("SELECT id FROM cash_withdrawals WHERE groupId = :groupId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncWithdrawalIds(groupId: String): List<String>

    /**
     * Deletes withdrawals whose IDs are in the provided list.
     * Used to selectively remove stale withdrawals during sync reconciliation.
     */
    @Query("DELETE FROM cash_withdrawals WHERE id IN (:ids)")
    suspend fun deleteWithdrawalsByIds(ids: List<String>)

    /**
     * Reconciles local withdrawals for a group with the authoritative cloud snapshot.
     *
     * Uses a merge strategy instead of destructive delete+insert:
     * 1. Capture non-SYNCED statuses (PENDING_SYNC / SYNC_FAILED) before upsert
     * 2. Upsert all remote withdrawals (adds new, updates existing — sets syncStatus to SYNCED)
     * 3. Restore ALL non-SYNCED statuses captured in step 1
     * 4. Delete only stale synced withdrawals (not in remote set AND fully synced)
     *
     * This preserves locally-created withdrawals that haven't synced to the cloud yet.
     *
     * **Why we always restore non-SYNCED statuses (even for items in the remote set):**
     * Firestore's `MetadataChanges.INCLUDE` fires snapshots that include pending
     * local writes (latency compensation). These items appear in the remote set
     * but have NOT been confirmed by the server. If we skip restoration for items
     * in the remote set, the upsert's default SYNCED status would overwrite
     * PENDING_SYNC — hiding the sync indicator. The PENDING_SYNC → SYNCED
     * transition is handled exclusively by the repository's explicit
     * `updateSyncStatus()` call after server confirmation (via
     * `confirmPendingSyncWithdrawals()` or the sync in `addWithdrawal()`).
     */
    @Transaction
    suspend fun replaceWithdrawalsForGroup(groupId: String, withdrawals: List<CashWithdrawalEntity>) {
        // Step 1: Capture non-SYNCED statuses before the upsert overwrites them
        val unsyncedStatuses = getUnsyncedWithdrawalStatuses(groupId)
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()

        val remoteIds = withdrawals.map { it.id }.toSet()
        val localIds = getWithdrawalIdsByGroupId(groupId)

        // Step 2: Upsert remote withdrawals (sets syncStatus to SYNCED for all)
        insertWithdrawals(withdrawals)

        // Step 3: Restore ALL non-SYNCED statuses that were captured before the upsert.
        // The upsert sets syncStatus to SYNCED for all items (including those that were
        // PENDING_SYNC or SYNC_FAILED). We must restore their original status because:
        // - Firestore snapshots with MetadataChanges.INCLUDE fire for pending writes
        //   (not yet confirmed by the server), so presence in remoteIds does NOT mean synced.
        // - The PENDING_SYNC → SYNCED transition is handled exclusively by the repository's
        //   explicit updateSyncStatus() call after confirmed cloud write.
        for (entry in unsyncedStatuses) {
            updateSyncStatus(entry.id, entry.syncStatus)
        }

        // Step 4: Remove stale withdrawals — only those that are NOT in the remote set
        // AND are NOT in a non-SYNCED state (protect unsynced local data)
        val staleIds = localIds.filter { it !in remoteIds && it !in unsyncedIds }
        if (staleIds.isNotEmpty()) {
            deleteWithdrawalsByIds(staleIds)
        }
    }
}
