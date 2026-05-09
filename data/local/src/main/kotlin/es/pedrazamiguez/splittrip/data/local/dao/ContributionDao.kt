package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.ContributionEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

/**
 * DAO for contribution records.
 *
 * The function count naturally exceeds Detekt's `TooManyFunctions` threshold because
 * a single entity DAO accumulates distinct query methods: inserts, lookups, deletions,
 * sync-status reads/writes, and the @Transaction reconciliation helper. All methods
 * are cohesive (they all operate on the `contributions` table) and splitting into
 * read/write DAOs would add architectural complexity with no real ISP benefit.
 */
@Suppress("TooManyFunctions")
@Dao
interface ContributionDao {

    @Upsert
    suspend fun insertContribution(contribution: ContributionEntity)

    @Upsert
    suspend fun insertContributions(contributions: List<ContributionEntity>)

    @Query("SELECT * FROM contributions WHERE groupId = :groupId ORDER BY createdAtMillis DESC")
    fun getContributionsByGroupIdFlow(groupId: String): Flow<List<ContributionEntity>>

    @Query("SELECT * FROM contributions WHERE id = :contributionId")
    suspend fun getContributionById(contributionId: String): ContributionEntity?

    @Query("DELETE FROM contributions WHERE id = :contributionId")
    suspend fun deleteContribution(contributionId: String)

    /**
     * Updates the sync status of a single contribution.
     * Used to transition between PENDING_SYNC → SYNCED or SYNC_FAILED after cloud sync.
     */
    @Query("UPDATE contributions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM contributions WHERE groupId = :groupId")
    suspend fun deleteContributionsByGroupId(groupId: String)

    @Query("SELECT id FROM contributions WHERE groupId = :groupId")
    suspend fun getContributionIdsByGroupId(groupId: String): List<String>

    @Query("DELETE FROM contributions")
    suspend fun clearAllContributions()

    @Query("DELETE FROM contributions WHERE groupId = :groupId AND linkedExpenseId = :expenseId")
    suspend fun deleteByLinkedExpenseId(groupId: String, expenseId: String)

    @Query(
        "SELECT * FROM contributions WHERE groupId = :groupId AND linkedExpenseId = :expenseId " +
            "ORDER BY COALESCE(lastUpdatedAtMillis, createdAtMillis, 0) DESC, id DESC LIMIT 1"
    )
    suspend fun findByLinkedExpenseId(groupId: String, expenseId: String): ContributionEntity?

    /**
     * Returns sync status metadata for contributions in a group that are NOT fully synced.
     * Used during cloud snapshot reconciliation to preserve PENDING_SYNC / SYNC_FAILED
     * statuses that would otherwise be overwritten by the upsert (which defaults to SYNCED).
     */
    @Query("SELECT id, syncStatus FROM contributions WHERE groupId = :groupId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedContributionStatuses(groupId: String): List<SyncStatusEntry>

    /**
     * Returns IDs of contributions in a group that are still waiting for server confirmation.
     * Used after reconciliation to attempt server verification and transition to SYNCED.
     */
    @Query("SELECT id FROM contributions WHERE groupId = :groupId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncContributionIds(groupId: String): List<String>

    /**
     * Deletes contributions whose IDs are in the provided list.
     * Used to selectively remove stale contributions during sync reconciliation.
     */
    @Query("DELETE FROM contributions WHERE id IN (:ids)")
    suspend fun deleteContributionsByIds(ids: List<String>)

    /**
     * Reconciles local contributions for a group with the authoritative cloud snapshot.
     *
     * Uses a merge strategy instead of destructive delete+insert:
     * 1. Capture non-SYNCED statuses (PENDING_SYNC / SYNC_FAILED) before upsert
     * 2. Upsert all remote contributions (adds new, updates existing — sets syncStatus to SYNCED)
     * 3. Restore ALL non-SYNCED statuses captured in step 1
     * 4. Delete only stale synced contributions (not in remote set AND fully synced)
     *
     * This preserves locally-created contributions that haven't synced to the cloud yet.
     *
     * **Why we always restore non-SYNCED statuses (even for items in the remote set):**
     * Firestore's `MetadataChanges.INCLUDE` fires snapshots that include pending
     * local writes (latency compensation). These items appear in the remote set
     * but have NOT been confirmed by the server. If we skip restoration for items
     * in the remote set, the upsert's default SYNCED status would overwrite
     * PENDING_SYNC — hiding the sync indicator. The PENDING_SYNC → SYNCED
     * transition is handled exclusively by the repository's explicit
     * `updateSyncStatus()` call after server confirmation (via
     * `confirmPendingSyncContributions()` or the sync in `addContribution()`).
     */
    @Transaction
    suspend fun replaceContributionsForGroup(groupId: String, contributions: List<ContributionEntity>) {
        // Step 1: Capture non-SYNCED statuses before the upsert overwrites them
        val unsyncedStatuses = getUnsyncedContributionStatuses(groupId)
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()

        val remoteIds = contributions.map { it.id }.toSet()
        val localIds = getContributionIdsByGroupId(groupId)

        // Step 2: Upsert remote contributions (sets syncStatus to SYNCED for all)
        insertContributions(contributions)

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

        // Step 4: Remove stale contributions — only those that are NOT in the remote set
        // AND are NOT in a non-SYNCED state (protect unsynced local data)
        val staleIds = localIds.filter { it !in remoteIds && it !in unsyncedIds }
        if (staleIds.isNotEmpty()) {
            deleteContributionsByIds(staleIds)
        }
    }
}
