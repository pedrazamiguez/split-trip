package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.sync.subscribeAndReconcile
import es.pedrazamiguez.splittrip.data.worker.GroupDeletionRetryScheduler
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
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
    private val groupImageStorageService: GroupImageStorageService,
    private val cloudStorageDataSource: CloudStorageDataSource,
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

        val finalLocalImagePath = commitTempImage(groupId, group.mainImagePath)
        val membersWithCreator = ensureCreatorInMembers(currentUserId, group.members)

        val createdGroup = group.copy(
            id = groupId,
            members = membersWithCreator,
            mainImagePath = finalLocalImagePath,
            createdAt = group.createdAt ?: currentTimestamp,
            lastUpdatedAt = currentTimestamp,
            syncStatus = SyncStatus.PENDING_SYNC
        )

        // Save to local FIRST - UI updates instantly
        localGroupDataSource.saveGroup(createdGroup)

        // Sync to cloud in background with two-phase verification
        syncScope.launch {
            syncCreatedGroupToCloud(groupId, createdGroup, finalLocalImagePath)
        }

        return groupId
    }

    private suspend fun commitTempImage(groupId: String, tempPath: String?): String? {
        if (tempPath.isNullOrBlank()) return null
        return try {
            groupImageStorageService.commitGroupImage(groupId, tempPath)
        } catch (e: Exception) {
            Timber.e(e, "Failed to commit group image $tempPath for group $groupId")
            null
        }
    }

    private fun ensureCreatorInMembers(userId: String, members: List<String>): List<String> {
        return if (userId !in members) members + userId else members
    }

    private suspend fun <T> runCloudOp(
        tag: String = "CloudOp",
        message: String = "Operation failed",
        block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "$tag: $message")
            null
        }
    }

    private suspend fun uploadAndSaveLocalImage(
        groupId: String,
        createdGroup: Group,
        localImagePath: String
    ): String? {
        val uploadedUrl = runCloudOp(
            tag = "GroupImageUpload",
            message = "Failed to upload group image to cloud storage for group $groupId"
        ) {
            cloudStorageDataSource.uploadGroupImage(groupId, localImagePath, "image/webp")
        } ?: return null

        try {
            val updatedLocalGroup = createdGroup.copy(mainImagePath = uploadedUrl)
            localGroupDataSource.saveGroup(updatedLocalGroup)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to save updated local group with image url for group $groupId")
        }
        return uploadedUrl
    }

    private suspend fun createGroupInFirestore(
        groupToSync: Group
    ): Boolean {
        return runCloudOp(
            tag = "FirestoreSync",
            message = "Failed to sync group to cloud"
        ) {
            cloudGroupDataSource.createGroup(groupToSync)
            true
        } ?: false
    }

    private suspend fun verifyGroupSyncOnServer(groupId: String) {
        runCloudOp(
            tag = "FirestoreVerify",
            message = "Group saved to Firestore cache, pending server confirmation: $groupId"
        ) {
            cloudGroupDataSource.verifyGroupOnServer(groupId)
            localGroupDataSource.updateSyncStatus(groupId, SyncStatus.SYNCED)
            Timber.d("Group synced and confirmed on server: $groupId")
            true
        }
    }

    private suspend fun syncCreatedGroupToCloud(
        groupId: String,
        createdGroup: Group,
        localImagePath: String?
    ) {
        val remoteImageUrl = if (localImagePath != null) {
            uploadAndSaveLocalImage(groupId, createdGroup, localImagePath)
        } else {
            null
        }

        val groupToSync = if (remoteImageUrl != null) {
            createdGroup.copy(mainImagePath = remoteImageUrl)
        } else {
            createdGroup
        }

        val createdSuccessfully = createGroupInFirestore(groupToSync)
        if (!createdSuccessfully) {
            // Only downgrade to SYNC_FAILED if the snapshot listener has not already
            // confirmed the entity as SYNCED (guards against the ACK-loss race condition).
            val currentStatus = localGroupDataSource.getGroupById(groupId)?.syncStatus
            if (currentStatus == SyncStatus.PENDING_SYNC) {
                localGroupDataSource.updateSyncStatus(groupId, SyncStatus.SYNC_FAILED)
            }
            return
        }

        verifyGroupSyncOnServer(groupId)
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

        // Delete local permanent group image folder/file
        groupImageStorageService.deleteLocalGroupImage(groupId)

        // 2. Signal Firestore to initiate server-side cascading delete.
        // requestGroupDeletion() uses a WriteBatch that atomically sets
        // deletionRequested=true AND deletes the current user's member doc.
        // The member-doc deletion is critical to prevent brief entity resurrection
        // when MetadataChanges.INCLUDE fires the group_members snapshot listener.
        // Always queue the cloud deletion, even for PENDING_SYNC entities.
        syncScope.launch {
            try {
                cloudStorageDataSource.deleteGroupImage(groupId)
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete remote group image for group: $groupId")
            }

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
