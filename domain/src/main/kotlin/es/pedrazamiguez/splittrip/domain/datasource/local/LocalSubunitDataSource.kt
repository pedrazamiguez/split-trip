package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import kotlinx.coroutines.flow.Flow

interface LocalSubunitDataSource {

    fun getSubunitsByGroupIdFlow(groupId: String): Flow<List<Subunit>>

    /**
     * One-shot read of all subunits for a group.
     * Does not trigger any sync or subscription side effects.
     */
    suspend fun getSubunitsByGroupId(groupId: String): List<Subunit>

    suspend fun saveSubunit(subunit: Subunit)

    suspend fun deleteSubunit(subunitId: String)

    suspend fun deleteSubunitsByGroupId(groupId: String)

    /**
     * Atomically replaces all subunits for a group with the provided list.
     * Used during real-time sync to reconcile local state with the cloud snapshot.
     */
    suspend fun replaceSubunitsForGroup(groupId: String, subunits: List<Subunit>)

    /**
     * Atomically applies an incremental delta (upserts + tombstone deletions) for a group.
     */
    suspend fun applySubunitsDeltaForGroup(
        groupId: String,
        upserts: List<Subunit>,
        deletedIds: List<String>
    )

    suspend fun getSubunitIdsByGroup(groupId: String): List<String>

    suspend fun getSubunitById(subunitId: String): Subunit?

    /**
     * Updates the sync status of a single subunit.
     * Used by repositories to track cloud sync progress (PENDING_SYNC → SYNCED / SYNC_FAILED).
     */
    suspend fun updateSyncStatus(subunitId: String, syncStatus: SyncStatus)

    /**
     * Returns IDs of subunits in a group that are waiting for server confirmation.
     * Used by the repository after reconciliation to attempt server verification
     * and transition PENDING_SYNC items to SYNCED.
     */
    suspend fun getPendingSyncSubunitIds(groupId: String): List<String>

    suspend fun clearAllSubunits()
}
