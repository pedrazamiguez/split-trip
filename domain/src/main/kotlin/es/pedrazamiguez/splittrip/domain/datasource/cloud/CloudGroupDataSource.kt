package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface CloudGroupDataSource {
    suspend fun createGroup(group: Group): String
    suspend fun getGroupById(groupId: String): Group?
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(groupId: String)

    /**
     * Signals Firestore to initiate a server-side cascading group deletion.
     *
     * Atomically (via WriteBatch):
     * 1. Sets `deletionRequested = true` on the group document — triggers the
     *    `onGroupDeletionRequested` Cloud Function to delete all subcollections
     *    and the group document itself.
     * 2. Deletes the current user's member document — prevents entity resurrection
     *    when the `group_members` snapshot listener (with `MetadataChanges.INCLUDE`)
     *    fires after the group creation batch is confirmed on reconnect.
     *
     * The operation is idempotent: calling this on a group that already has
     * `deletionRequested = true` is a safe no-op for the Cloud Function trigger
     * (its guard condition prevents re-execution). The Cloud Function handles
     * missing member docs gracefully.
     *
     * @param groupId The ID of the group to request deletion for.
     * @throws Exception if the Firestore batch commit fails. The caller
     *   should schedule a retry via [GroupDeletionRetryScheduler].
     */
    suspend fun requestGroupDeletion(groupId: String)

    /**
     * One-shot fetch of all groups for sync purposes.
     * Backed by a Firestore .get().await() call that uses the default source
     * (server when available, but may fall back to the local cache).
     * Exceptions propagate to the caller; use this for background sync operations
     * instead of the reactive Flow.
     */
    fun getGroupFlow(groupId: String): Flow<Group?>

    suspend fun fetchAllGroups(): List<Group>

    /**
     * Reactive stream of groups for real-time UI observers.
     * Emits local cache first, then server data as it arrives.
     */
    fun getAllGroupsFlow(): Flow<List<Group>>

    /**
     * Verifies that a group document exists on the Firestore server (not just local cache).
     * Forces a server round-trip — throws if the device is offline.
     *
     * Used by repositories to confirm that a locally-created group has been
     * successfully persisted to the server, enabling the PENDING_SYNC → SYNCED transition.
     *
     * @param groupId The ID of the group to verify.
     * @return true if the group exists on the server.
     * @throws Exception if the server is unreachable (e.g., airplane mode).
     */
    suspend fun verifyGroupOnServer(groupId: String): Boolean

    /**
     * Replaces pending user IDs with active user IDs across group memberships, expenses, splits, contributions, and withdrawals in Firestore.
     */
    suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String)

    /**
     * Removes a user from a group in Firestore.
     *
     * Uses a WriteBatch to atomically:
     * 1. Remove [userId] from the group document's `memberIds` array
     * 2. Delete the user's member document from `groups/{groupId}/members/{userId}`
     *
     * The member-doc deletion is critical: the `getAllGroupsFlow()` snapshot listener
     * uses `collectionGroup("members")` to determine group membership. Without deleting
     * the member doc, the listener would continue to push the group to the leaving user,
     * causing sync delegates to upsert the stale cloud data back into Room.
     *
     * @param groupId The ID of the group to leave.
     * @param userId The ID of the user leaving the group.
     */
    suspend fun leaveGroup(groupId: String, userId: String)
}
