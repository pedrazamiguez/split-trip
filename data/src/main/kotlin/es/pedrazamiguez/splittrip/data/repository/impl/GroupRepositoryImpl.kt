package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.sync.subscribeAndReconcile
import es.pedrazamiguez.splittrip.data.worker.GroupDeletionRetryScheduler
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Offline-First implementation of GroupRepository.
 *
 * Pattern: Single Source of Truth
 * - The UI ALWAYS reads from the local database (Room)
 * - The repository syncs with the cloud in the background
 * - If sync fails (no internet), the user still sees local data
 */
class GroupRepositoryImpl(
    private val cloudGroupDataSource: CloudGroupDataSource,
    private val localGroupDataSource: LocalGroupDataSource,
    private val authenticationService: AuthenticationService,
    private val groupDeletionRetryScheduler: GroupDeletionRetryScheduler,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GroupRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    /**
     * Tracks the single active cloud subscription Job for groups.
     * Prevents duplicate Firestore snapshot listeners from accumulating
     * when onStart fires multiple times (e.g., config changes, tab switches,
     * WhileSubscribed resubscriptions).
     */
    private var cloudSubscriptionJob: Job? = null

    /**
     * Returns a Flow of groups from local storage.
     * On start, subscribes to real-time cloud changes for multi-user sync.
     * This is INSTANT because data comes from Room.
     *
     * Uses a single shared subscription: any existing cloud listener is cancelled
     * before starting a new one, preventing duplicate snapshot listeners.
     */
    override fun getAllGroupsFlow(): Flow<List<Group>> = localGroupDataSource.getGroupsFlow()
        .onStart {
            cloudSubscriptionJob?.cancel()
            cloudSubscriptionJob = syncScope.launch {
                subscribeAndReconcile(
                    cloudFlow = cloudGroupDataSource.getAllGroupsFlow(),
                    reconcileLocal = localGroupDataSource::replaceAllGroups,
                    getPendingIds = localGroupDataSource::getPendingSyncGroupIds,
                    verifyOnServer = cloudGroupDataSource::verifyGroupOnServer,
                    markSynced = { id ->
                        localGroupDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                    },
                    entityLabel = "group",
                    logContext = ""
                )
            }
        }

    /**
     * Gets a group by ID.
     * First tries local, then falls back to cloud if not found locally.
     */
    override suspend fun getGroupById(groupId: String): Group? {
        // Try local first (instant)
        localGroupDataSource.getGroupById(groupId)?.let { return it }

        // If not found locally, try cloud and cache it
        return try {
            cloudGroupDataSource.getGroupById(groupId)?.also { group ->
                localGroupDataSource.saveGroup(group)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching group from cloud: $groupId")
            null
        }
    }

    /**
     * Creates a group locally first, then syncs to cloud.
     * Ensures offline support by saving to local database before cloud sync.
     *
     * The sync status transitions follow a two-phase verification:
     * 1. Cloud write: batch commit to Firestore (may resolve from local cache if offline)
     * 2. Server verification: Source.SERVER read to confirm the write reached the server
     *
     * - Online: both succeed → SYNCED
     * - Offline: write cached locally, verification fails → stays PENDING_SYNC
     * - Error: write rejected by Firestore (permissions, etc.) → SYNC_FAILED
     */
    override suspend fun createGroup(group: Group): String {
        val groupId = UUID.randomUUID().toString()
        val currentTimestamp = LocalDateTime.now()
        val currentUserId = authenticationService.requireUserId()

        // Ensure the creator is always in the members list.
        // This is enforced here (repository layer) so it applies to all callers
        // and the locally-saved Group is consistent with what Firestore will have.
        val membersWithCreator = if (currentUserId !in group.members) {
            group.members + currentUserId
        } else {
            group.members
        }

        val createdGroup = group.copy(
            id = groupId,
            members = membersWithCreator,
            createdAt = group.createdAt ?: currentTimestamp,
            lastUpdatedAt = currentTimestamp,
            syncStatus = SyncStatus.PENDING_SYNC
        )

        // Save to local FIRST - UI updates instantly
        localGroupDataSource.saveGroup(createdGroup)

        // Sync to cloud in background with two-phase verification
        syncScope.launch {
            // Phase 1: Write to Firestore (resolves from local cache if offline)
            try {
                cloudGroupDataSource.createGroup(createdGroup)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Only downgrade to SYNC_FAILED if the snapshot listener has not already
                // confirmed the entity as SYNCED (guards against the ACK-loss race condition).
                val currentStatus = localGroupDataSource.getGroupById(groupId)?.syncStatus
                if (currentStatus == SyncStatus.PENDING_SYNC) {
                    localGroupDataSource.updateSyncStatus(groupId, SyncStatus.SYNC_FAILED)
                }
                Timber.w(e, "Failed to sync group to cloud")
                return@launch
            }

            // Phase 2: Verify the write reached the server (Source.SERVER round-trip)
            try {
                cloudGroupDataSource.verifyGroupOnServer(groupId)
                localGroupDataSource.updateSyncStatus(groupId, SyncStatus.SYNCED)
                Timber.d("Group synced and confirmed on server: $groupId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Server unreachable — write is cached in Firestore, will sync automatically.
                // Keep as PENDING_SYNC (already the current status from local save).
                // The confirmPendingSyncGroups() mechanism will transition to SYNCED
                // when the snapshot listener re-fires after server confirmation.
                Timber.d(
                    e,
                    "Group saved to Firestore cache, pending server confirmation: $groupId"
                )
            }
        }

        return groupId
    }

    /**
     * Deletes a group using a server-side cascading delete strategy.
     *
     * Flow:
     * 1. Delete group from Room — FK CASCADE handles all child entities locally.
     *    UI updates instantly.
     * 2. Signal Firestore via `requestGroupDeletion()` which atomically
     *    (WriteBatch) sets `deletionRequested = true` on the group document
     *    AND deletes the current user's member document.
     */
    override suspend fun deleteGroup(groupId: String) {
        // 1. Delete from Room immediately — FK CASCADE handles child entities.
        // UI updates instantly via the observed Room Flow.
        localGroupDataSource.deleteGroup(groupId)

        // 2. Signal Firestore to initiate server-side cascading delete.
        // requestGroupDeletion() uses a WriteBatch that atomically sets
        // deletionRequested=true AND deletes the current user's member doc.
        // The member-doc deletion is critical to prevent brief entity resurrection
        // when MetadataChanges.INCLUDE fires the group_members snapshot listener.
        // Always queue the cloud deletion, even for PENDING_SYNC entities.
        syncScope.launch {
            try {
                cloudGroupDataSource.requestGroupDeletion(groupId)
                Timber.d("Group deletion requested for cloud: $groupId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to request cloud deletion for group: $groupId")
                groupDeletionRetryScheduler.scheduleRetry(groupId)
            }
        }
    }
}
