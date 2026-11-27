package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import kotlinx.coroutines.flow.Flow

interface LocalContributionDataSource {

    fun getContributionsByGroupIdFlow(groupId: String): Flow<List<Contribution>>

    suspend fun saveContribution(contribution: Contribution)

    /**
     * Finds a contribution by its ID, or null if not found.
     */
    suspend fun findContributionById(contributionId: String): Contribution?

    suspend fun deleteContribution(contributionId: String)

    suspend fun deleteContributionsByGroupId(groupId: String)

    /**
     * Atomically replaces all contributions for a group with the provided list.
     * Used during real-time sync to reconcile local state with the cloud snapshot.
     */
    suspend fun replaceContributionsForGroup(groupId: String, contributions: List<Contribution>)

    /**
     * Atomically applies an incremental delta (upserts + tombstone deletions) for a group.
     */
    suspend fun applyContributionsDeltaForGroup(
        groupId: String,
        upserts: List<Contribution>,
        deletedIds: List<String>
    )

    suspend fun getContributionIdsByGroup(groupId: String): List<String>

    /**
     * Updates the sync status of a single contribution.
     * Used by repositories to track cloud sync progress (PENDING_SYNC → SYNCED / SYNC_FAILED).
     */
    suspend fun updateSyncStatus(contributionId: String, syncStatus: SyncStatus)

    /**
     * Returns IDs of contributions in a group that are waiting for server confirmation.
     * Used by the repository after reconciliation to attempt server verification
     * and transition PENDING_SYNC items to SYNCED.
     */
    suspend fun getPendingSyncContributionIds(groupId: String): List<String>

    suspend fun clearAllContributions()

    /**
     * Deletes the contribution paired with the given expense ID within the specified group.
     *
     * The domain model expects at most one linked contribution for a given
     * `(groupId, linkedExpenseId)` pair. Used for cascade-deletion when an
     * out-of-pocket expense is deleted.
     */
    suspend fun deleteByLinkedExpenseId(groupId: String, linkedExpenseId: String)

    /**
     * Finds the contribution linked to the given expense ID within the specified group,
     * or null if none exists.
     *
     * The domain model expects at most one linked contribution for a given
     * `(groupId, linkedExpenseId)` pair. Used for edit/update support of
     * out-of-pocket paired contributions.
     */
    suspend fun findByLinkedExpenseId(groupId: String, linkedExpenseId: String): Contribution?
}
