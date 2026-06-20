package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Group entities.
 * Provides reactive access to locally stored groups using Flow.
 */
@Dao
interface GroupDao {

    /**
     * Observes all groups in the database.
     * This Flow emits automatically whenever the groups table changes.
     * This is the foundation of the Offline-First pattern - UI always reads from here.
     */
    @Query("SELECT * FROM groups ORDER BY lastUpdatedAtMillis DESC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    /**
     * Gets all groups from the database.
     */
    @Query("SELECT * FROM groups")
    suspend fun getAllGroups(): List<GroupEntity>

    /**
     * Gets a single group by ID.
     */
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): GroupEntity?

    /**
     * Observes a single group by ID.
     */
    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroupByIdFlow(groupId: String): Flow<GroupEntity?>

    /**
     * Inserts or updates groups.
     * Uses @Upsert to perform a true UPDATE if the ID exists, INSERT if not.
     * This prevents DELETE+INSERT behavior of REPLACE, which would trigger
     * CASCADE deletion of related expenses via ForeignKey constraints.
     */
    @Upsert
    suspend fun insertGroups(groups: List<GroupEntity>)

    /**
     * Inserts or updates a single group.
     */
    @Upsert
    suspend fun insertGroup(group: GroupEntity)

    /**
     * Deletes all groups from the table.
     */
    @Query("DELETE FROM groups")
    suspend fun clearAllGroups()

    /**
     * Deletes a specific group by ID.
     */
    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    /**
     * Updates the sync status of a single group.
     * Used to transition between PENDING_SYNC → SYNCED or SYNC_FAILED after cloud sync.
     */
    @Query("UPDATE groups SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    /**
     * Retrieves all group IDs currently stored locally.
     * Used during reconciliation to identify stale groups.
     */
    @Query("SELECT id FROM groups")
    suspend fun getAllGroupIds(): List<String>

    /**
     * Returns sync status metadata for all groups that are NOT fully synced.
     * Used during cloud snapshot reconciliation to preserve PENDING_SYNC / SYNC_FAILED
     * statuses that would otherwise be overwritten by the upsert (which defaults to SYNCED).
     */
    @Query("SELECT id, syncStatus FROM groups WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedGroupStatuses(): List<SyncStatusEntry>

    /**
     * Returns IDs of groups that are still waiting for server confirmation.
     * Used after reconciliation to attempt server verification and transition to SYNCED.
     */
    @Query("SELECT id FROM groups WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncGroupIds(): List<String>

    /**
     * Deletes groups whose IDs are in the provided list.
     * Used to selectively remove stale groups during sync reconciliation.
     */
    @Query("DELETE FROM groups WHERE id IN (:ids)")
    suspend fun deleteGroupsByIds(ids: List<String>)

    /**
     * Reconciles local groups with the authoritative cloud snapshot.
     *
     * Uses a merge strategy instead of destructive delete+insert:
     * 1. Capture non-SYNCED statuses (PENDING_SYNC / SYNC_FAILED) before upsert
     * 2. Upsert all remote groups (adds new, updates existing — sets syncStatus to SYNCED)
     * 3. Restore ALL non-SYNCED statuses captured in step 1
     * 4. Delete only stale synced groups (not in remote set AND fully synced)
     *
     * This preserves locally-created groups that haven't synced to the cloud yet:
     * - Their syncStatus (PENDING_SYNC / SYNC_FAILED) is restored after upsert
     * - They are protected from stale deletion even if not in the remote snapshot
     *
     * **Why we always restore non-SYNCED statuses (even for items in the remote set):**
     * Firestore's `MetadataChanges.INCLUDE` fires snapshots that include pending
     * local writes (latency compensation). These items appear in the remote set
     * but have NOT been confirmed by the server. If we skip restoration for items
     * in the remote set, the upsert's default SYNCED status would overwrite
     * PENDING_SYNC — hiding the sync indicator. The PENDING_SYNC → SYNCED
     * transition is handled exclusively by the repository's explicit
     * `updateSyncStatus()` call after server confirmation (via
     * `confirmPendingSyncGroups()` or the two-phase sync in `createGroup()`).
     */
    @Transaction
    suspend fun replaceAllGroups(groups: List<GroupEntity>) {
        // Step 1: Capture non-SYNCED statuses before the upsert overwrites them
        val unsyncedStatuses = getUnsyncedGroupStatuses()
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()

        val remoteIds = groups.map { it.id }.toSet()
        val localIds = getAllGroupIds()

        val existingGroups = localIds.mapNotNull { getGroupById(it) }.associateBy { it.id }
        val mergedGroups = groups.map { remote ->
            val local = existingGroups[remote.id]
            if (local != null) {
                remote.copy(
                    mainImagePath = remote.mainImagePath?.takeIf { it.isNotBlank() } ?: local.mainImagePath
                )
            } else {
                remote
            }
        }

        // Step 2: Upsert remote groups (sets syncStatus to SYNCED for all)
        insertGroups(mergedGroups)

        // Step 3: Restore ALL non-SYNCED statuses that were captured before the upsert.
        // The upsert sets syncStatus to SYNCED for all items (including those that were
        // PENDING_SYNC or SYNC_FAILED). We must restore their original status because:
        // - Firestore snapshots with MetadataChanges.INCLUDE fire for pending writes
        //   (not yet confirmed by the server), so presence in remoteIds does NOT mean synced.
        // - The PENDING_SYNC → SYNCED transition is handled exclusively by the repository
        //   (confirmPendingSyncGroups / two-phase createGroup), not by reconciliation.
        for (entry in unsyncedStatuses) {
            updateSyncStatus(entry.id, entry.syncStatus)
        }

        // Step 4: Remove stale groups — only those that are NOT in the remote set
        // AND are NOT in a non-SYNCED state (protect unsynced local data)
        val staleIds = localIds.filter { it !in remoteIds && it !in unsyncedIds }
        if (staleIds.isNotEmpty()) {
            deleteGroupsByIds(staleIds)
        }
    }
}
