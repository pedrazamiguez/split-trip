package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Subscribes to a real-time cloud [Flow] and reconciles the local database
 * on each emission. After reconciliation, attempts to confirm any PENDING_SYNC
 * items via server verification.
 *
 * Encapsulates the common `subscribeToCloudChanges()` + `confirmPendingSyncXxx()`
 * pattern shared by all 5 entity repositories.
 *
 * @param T The domain entity type.
 * @param cloudFlow The Firestore snapshot listener Flow emitting the complete
 *   authoritative state of the collection.
 * @param reconcileLocal Merge reconciliation function (upsert remote + selective
 *   delete of stale). Typically calls `replaceXxxForGroup()`.
 * @param getPendingIds Returns IDs of locally-stored entities with
 *   `syncStatus == PENDING_SYNC`.
 * @param verifyOnServer Performs a `Source.SERVER` read to confirm the entity
 *   exists on the server (not just in the local Firestore cache).
 * @param markSynced Updates the entity's sync status to [SyncStatus.SYNCED].
 * @param entityLabel Human-readable entity name for Timber logging
 *   (e.g., "expense", "subunit").
 * @param logContext Additional context appended to log messages
 *   (e.g., "for group abc-123").
 */
internal suspend fun <T> subscribeAndReconcile(
    cloudFlow: Flow<List<T>>,
    reconcileLocal: suspend (List<T>) -> Unit,
    getPendingIds: suspend () -> List<String>,
    verifyOnServer: suspend (String) -> Boolean,
    markSynced: suspend (String) -> Unit,
    entityLabel: String,
    logContext: String
) {
    try {
        cloudFlow.collect { remoteItems ->
            try {
                Timber.tag(LogTag.SYNC).d("Real-time sync: %d %ss %s", remoteItems.size, entityLabel, logContext)
                reconcileLocal(remoteItems)
                confirmPendingSync(getPendingIds, verifyOnServer, markSynced, entityLabel)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(LogTag.SYNC).w(e, "Error reconciling %ss from cloud snapshot", entityLabel)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(e, "Error subscribing to cloud %s changes, using local cache", entityLabel)
    }
}

/**
 * Attempts to confirm PENDING_SYNC entities by verifying their existence
 * on the server.
 *
 * Called after each reconciliation cycle. When the device is online and
 * Firestore has confirmed the pending write, the server verification
 * succeeds and the entity transitions to SYNCED. When offline, the
 * verification throws and the entity remains PENDING_SYNC.
 *
 * @param getPendingIds Returns IDs of locally-stored entities with
 *   `syncStatus == PENDING_SYNC`.
 * @param verifyOnServer Performs a `Source.SERVER` read to confirm the
 *   entity exists on the server.
 * @param markSynced Updates the entity's sync status to [SyncStatus.SYNCED].
 * @param entityLabel Human-readable entity name for Timber logging.
 */
internal suspend fun confirmPendingSync(
    getPendingIds: suspend () -> List<String>,
    verifyOnServer: suspend (String) -> Boolean,
    markSynced: suspend (String) -> Unit,
    entityLabel: String
) {
    val pendingIds = getPendingIds()
    if (pendingIds.isEmpty()) return

    for (id in pendingIds) {
        try {
            if (verifyOnServer(id)) {
                markSynced(id)
                Timber.tag(LogTag.SYNC).d("Confirmed %s sync: %s", entityLabel, id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(LogTag.SYNC).d(e, "Cannot confirm %s %s — server unreachable", entityLabel, id)
        }
    }
}

/**
 * Launches a background coroutine that pushes a locally-saved entity to the
 * cloud and transitions its sync status accordingly.
 *
 * - Success: `updateSyncStatus(id, SYNCED)`
 * - Failure: `updateSyncStatus(id, SYNC_FAILED)` — only if the entity is still
 *   [SyncStatus.PENDING_SYNC] (guarded by [getCurrentSyncStatus] when provided).
 *
 * The optional [getCurrentSyncStatus] guard prevents a false [SyncStatus.SYNC_FAILED]
 * flash when the Firestore snapshot listener has already reconciled the entity to
 * [SyncStatus.SYNCED] before the write-path ACK roundtrip completes (ACK-loss race).
 *
 * @param scope CoroutineScope for the background sync job (typically `syncScope`).
 * @param entityId ID of the entity being synced.
 * @param cloudWrite Suspend function that performs the cloud write
 *   (e.g., `cloudDataSource.addExpense(groupId, entity)`).
 * @param updateSyncStatus Suspend function that updates the local entity's
 *   sync status.
 * @param getCurrentSyncStatus Optional suspend function that reads the entity's
 *   current sync status from local storage. When provided, [SyncStatus.SYNC_FAILED]
 *   is only written if the status is still [SyncStatus.PENDING_SYNC]. When `null`
 *   (default), the old unconditional behaviour is preserved for backward compatibility.
 * @param entityLabel Human-readable entity name for Timber logging.
 */
internal fun syncCreateToCloud(
    scope: CoroutineScope,
    entityId: String,
    cloudWrite: suspend () -> Unit,
    updateSyncStatus: suspend (String, SyncStatus) -> Unit,
    getCurrentSyncStatus: (suspend (String) -> SyncStatus)? = null,
    entityLabel: String
) {
    scope.launch {
        try {
            cloudWrite()
            updateSyncStatus(entityId, SyncStatus.SYNCED)
            Timber.tag(LogTag.SYNC).d(
                "%s synced to cloud: %s",
                entityLabel.replaceFirstChar {
                    it.uppercase()
                },
                entityId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Only downgrade to SYNC_FAILED if the snapshot listener has not already
            // confirmed the entity as SYNCED (guards against the ACK-loss race condition).
            // When getCurrentSyncStatus is null (not provided), fall back to old behaviour.
            val currentStatus = getCurrentSyncStatus?.invoke(entityId)
            if (currentStatus == null || currentStatus == SyncStatus.PENDING_SYNC) {
                updateSyncStatus(entityId, SyncStatus.SYNC_FAILED)
            }
            Timber.tag(LogTag.SYNC).w(e, "Failed to sync %s to cloud", entityLabel)
        }
    }
}

/**
 * Launches a background coroutine that deletes an entity from the cloud.
 *
 * Always queues the cloud deletion, even for PENDING_SYNC entities.
 * Firestore SDK guarantees write ordering: the queued SET (from creation)
 * executes before this DELETE when connectivity is restored.
 *
 * @param scope CoroutineScope for the background sync job (typically `syncScope`).
 * @param entityId ID of the entity being deleted.
 * @param cloudDelete Suspend function that performs the cloud deletion.
 * @param entityLabel Human-readable entity name for Timber logging.
 */
internal fun syncDeletionToCloud(
    scope: CoroutineScope,
    entityId: String,
    cloudDelete: suspend () -> Unit,
    entityLabel: String
) {
    scope.launch {
        try {
            cloudDelete()
            Timber.tag(LogTag.SYNC).d(
                "%s deletion synced to cloud: %s",
                entityLabel.replaceFirstChar {
                    it.uppercase()
                },
                entityId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(LogTag.SYNC).w(e, "Failed to sync %s deletion to cloud, will retry later", entityLabel)
        }
    }
}
