@file:Suppress("MatchingDeclarationName")

package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.core.performance.PerformanceTraces
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("MatchingDeclarationName")
internal data class SyncReconciliationParams<T>(
    val reconcileLocal: suspend (List<T>) -> Unit,
    val getPendingIds: suspend () -> List<String>,
    val verifyOnServer: suspend (String) -> Boolean,
    val markSynced: suspend (String) -> Unit,
    val entityLabel: String,
    val logContext: String,
    val performanceMonitor: PerformanceMonitor,
    val verifyParentExists: (suspend () -> Boolean)? = null
)

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
 * @param params Configuration parameters for reconciliation, server verification, logging, and monitoring.
 */
internal suspend fun <T> subscribeAndReconcile(
    cloudFlow: Flow<List<T>>,
    params: SyncReconciliationParams<T>
) {
    try {
        cloudFlow.collect { remoteItems ->
            try {
                if (params.verifyParentExists != null && !params.verifyParentExists.invoke()) {
                    Timber.tag(LogTag.SYNC).w("Parent missing, skipping reconciliation for %ss", params.entityLabel)
                    return@collect
                }
                params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_SUBSCRIBE_AND_RECONCILE) {
                    Timber.tag(LogTag.SYNC).v(
                        "Real-time sync: %d %ss %s",
                        remoteItems.size,
                        params.entityLabel,
                        params.logContext
                    )
                    params.reconcileLocal(remoteItems)
                    confirmPendingSync(
                        params.getPendingIds,
                        params.verifyOnServer,
                        params.markSynced,
                        params.entityLabel
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(LogTag.SYNC).w(e, "Error reconciling %ss from cloud snapshot", params.entityLabel)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(e, "Error subscribing to cloud %s changes, using local cache", params.entityLabel)
    }
}

/**
 * Performs a one-shot fetch of a group's collection (used for read-only ARCHIVED groups)
 * and reconciles the local database once per session.
 *
 * Avoids attaching a persistent Firestore snapshot listener for archived groups while
 * still hydrating local storage if the user opens an archived group not yet cached.
 */
internal suspend fun <T> fetchOnceAndReconcile(
    groupId: String,
    syncedArchivedGroupIds: MutableSet<String>,
    fetchOnce: suspend () -> List<T>,
    params: SyncReconciliationParams<T>
) {
    if (groupId in syncedArchivedGroupIds) return
    try {
        val remoteItems = fetchOnce()
        if (params.verifyParentExists != null && !params.verifyParentExists.invoke()) {
            Timber.tag(LogTag.SYNC).w(
                "Parent missing, skipping one-shot reconciliation for %ss",
                params.entityLabel
            )
            return
        }
        params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_SUBSCRIBE_AND_RECONCILE) {
            Timber.tag(LogTag.SYNC).v(
                "One-shot archived sync: %d %ss %s",
                remoteItems.size,
                params.entityLabel,
                params.logContext
            )
            params.reconcileLocal(remoteItems)
            syncedArchivedGroupIds.add(groupId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(e, "Error fetching archived %ss, using local cache", params.entityLabel)
    }
}

/**
 * Routes group subcollection sync based on group status:
 * - ARCHIVED groups perform a one-shot fetch once per session via [fetchOnceAndReconcile].
 * - ACTIVE groups attach a reference-counted real-time listener via [subscribeAndReconcile].
 */
internal suspend fun <T> syncGroupCollection(
    groupId: String,
    isGroupArchived: suspend () -> Boolean,
    syncedArchivedGroupIds: MutableSet<String>,
    fetchOnce: suspend () -> List<T>,
    cloudFlow: () -> Flow<List<T>>,
    params: SyncReconciliationParams<T>
) {
    if (isGroupArchived()) {
        fetchOnceAndReconcile(
            groupId = groupId,
            syncedArchivedGroupIds = syncedArchivedGroupIds,
            fetchOnce = fetchOnce,
            params = params
        )
    } else {
        subscribeAndReconcile(
            cloudFlow = cloudFlow(),
            params = params
        )
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

@Suppress("MatchingDeclarationName")
internal data class SyncCreateParams(
    val cloudWrite: suspend () -> Unit,
    val updateSyncStatus: suspend (String, SyncStatus) -> Unit,
    val getCurrentSyncStatus: (suspend (String) -> SyncStatus)? = null,
    val entityLabel: String,
    val performanceMonitor: PerformanceMonitor,
    val telemetryTracker: TelemetryTracker? = null
)

/**
 * Launches a background coroutine that pushes a locally-saved entity to the
 * cloud and transitions its sync status accordingly.
 *
 * - Success: `params.updateSyncStatus(id, SYNCED)`
 * - Failure: `params.updateSyncStatus(id, SYNC_FAILED)` — only if the entity is still
 *   [SyncStatus.PENDING_SYNC] (guarded by [SyncCreateParams.getCurrentSyncStatus] when provided).
 *
 * The optional [SyncCreateParams.getCurrentSyncStatus] guard prevents a false [SyncStatus.SYNC_FAILED]
 * flash when the Firestore snapshot listener has already reconciled the entity to
 * [SyncStatus.SYNCED] before the write-path ACK roundtrip completes (ACK-loss race).
 *
 * @param scope CoroutineScope for the background sync job (typically `syncScope`).
 * @param entityId ID of the entity being synced.
 * @param params Configuration parameters for the cloud write, local status updates, logging, and monitoring.
 */
internal fun syncCreateToCloud(
    scope: CoroutineScope,
    entityId: String,
    params: SyncCreateParams
) {
    scope.launch {
        try {
            params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_CREATE_TO_CLOUD) {
                params.cloudWrite()
            }
            params.updateSyncStatus(entityId, SyncStatus.SYNCED)
            Timber.tag(LogTag.SYNC).d(
                "%s synced to cloud: %s",
                params.entityLabel.replaceFirstChar {
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
            val currentStatus = params.getCurrentSyncStatus?.invoke(entityId)
            if (currentStatus == null || currentStatus == SyncStatus.PENDING_SYNC) {
                params.updateSyncStatus(entityId, SyncStatus.SYNC_FAILED)
            }
            params.telemetryTracker?.trackEvent(
                "cloud_sync_failed",
                mapOf(
                    "entity" to params.entityLabel,
                    "entity_id" to entityId,
                    "reason" to (e.message ?: "unknown")
                )
            )
            Timber.tag(LogTag.SYNC).w(e, "Failed to sync %s to cloud", params.entityLabel)
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
 * @param telemetryTracker Optional tracker for cloud sync failure telemetry.
 */
internal fun syncDeletionToCloud(
    scope: CoroutineScope,
    entityId: String,
    cloudDelete: suspend () -> Unit,
    entityLabel: String,
    telemetryTracker: TelemetryTracker? = null
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
            telemetryTracker?.trackEvent(
                "cloud_sync_failed",
                mapOf(
                    "entity" to entityLabel,
                    "entity_id" to entityId,
                    "reason" to (e.message ?: "unknown")
                )
            )
            Timber.tag(LogTag.SYNC).w(e, "Failed to sync %s deletion to cloud, will retry later", entityLabel)
        }
    }
}
