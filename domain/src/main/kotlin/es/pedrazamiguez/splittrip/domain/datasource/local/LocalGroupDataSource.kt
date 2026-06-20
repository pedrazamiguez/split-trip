package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.coroutines.flow.Flow

/**
 * Local data source interface for Group entities.
 * This is part of the Offline-First architecture where Room serves
 * as the Single Source of Truth for the UI.
 */
interface LocalGroupDataSource {

    /**
     * Observes all groups from the local database.
     * This Flow emits whenever the groups table changes.
     */
    fun getGroupsFlow(): Flow<List<Group>>

    /**
     * Gets a single group by ID from local storage.
     */
    suspend fun getGroupById(groupId: String): Group?

    /**
     * Observes a single group by ID.
     */
    fun getGroupByIdFlow(groupId: String): Flow<Group?>

    /**
     * Saves groups to local storage.
     * Existing groups with the same ID will be replaced.
     */
    suspend fun saveGroups(groups: List<Group>)

    /**
     * Saves a single group to local storage.
     */
    suspend fun saveGroup(group: Group)

    /**
     * Replaces all groups atomically.
     * Useful for full sync operations.
     */
    suspend fun replaceAllGroups(groups: List<Group>)

    /**
     * Deletes a group from local storage.
     */
    suspend fun deleteGroup(groupId: String)

    /**
     * Updates the sync status of a single group.
     * Used by repositories to track cloud sync progress (PENDING_SYNC → SYNCED / SYNC_FAILED).
     */
    suspend fun updateSyncStatus(groupId: String, syncStatus: SyncStatus)

    /**
     * Returns IDs of groups that are waiting for server confirmation.
     * Used by the repository after reconciliation to attempt server verification
     * and transition PENDING_SYNC items to SYNCED.
     */
    suspend fun getPendingSyncGroupIds(): List<String>

    /**
     * Clears all groups from local storage.
     */
    suspend fun clearAllGroups()

    /**
     * Replaces pending user IDs with active user IDs in local group members lists.
     */
    suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String)
}
