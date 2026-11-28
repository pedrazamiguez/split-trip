@file:Suppress("MatchingDeclarationName")

package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.core.performance.PerformanceTraces
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import timber.log.Timber

internal object SyncCheckpointPolicy {
    const val CHECKPOINT_MAX_AGE_MILLIS: Long = 30L * 24L * 60L * 60L * 1_000L

    fun isExpired(watermark: SyncWatermark, nowMillis: Long): Boolean =
        (nowMillis - watermark.toEpochMillis()) > CHECKPOINT_MAX_AGE_MILLIS
}

@Suppress("MatchingDeclarationName")
internal data class DeltaSyncParams<T>(
    val groupId: String,
    val collectionName: SyncCollectionName,
    val checkpointDataSource: LocalSyncCheckpointDataSource,
    val isGroupArchived: suspend () -> Boolean,
    val syncedArchivedGroupIds: MutableSet<String>,
    val fetchFullSnapshot: suspend () -> DeltaSyncBatch<T>,
    val fetchDeltaOnce: suspend (SyncWatermark) -> DeltaSyncBatch<T>,
    val observeDeltaFlow: (SyncWatermark?) -> Flow<DeltaSyncBatch<T>>,
    val replaceLocal: suspend (List<T>) -> Unit,
    val applyDeltaLocal: suspend (upserts: List<T>, deletedIds: List<String>) -> Unit,
    val getPendingIds: suspend () -> List<String>,
    val verifyOnServer: suspend (String) -> Boolean,
    val markSynced: suspend (String) -> Unit,
    val entityLabel: String,
    val logContext: String,
    val performanceMonitor: PerformanceMonitor,
    val verifyParentExists: (suspend () -> Boolean)? = null,
    val onAfterReconcile: (suspend () -> Unit)? = null,
    val clockMillis: () -> Long = { System.currentTimeMillis() }
)

internal suspend fun <T> syncGroupCollectionDelta(params: DeltaSyncParams<T>) {
    var currentWatermark = resolveEffectiveWatermark(params)

    if (params.isGroupArchived()) {
        if (params.groupId in params.syncedArchivedGroupIds) return
        fetchOnceWithDeltaSync(currentWatermark, params)
        return
    }

    if (currentWatermark == null) {
        val bootstrapResult = bootstrapFullSnapshot(params)
        if (!bootstrapResult.shouldContinue) return
        currentWatermark = bootstrapResult.watermark
    }

    runRealtimeDeltaLoop(currentWatermark, params)
}

private suspend fun <T> resolveEffectiveWatermark(params: DeltaSyncParams<T>): SyncWatermark? {
    val stored = params.checkpointDataSource.getWatermark(params.groupId, params.collectionName)
    if (stored != null && SyncCheckpointPolicy.isExpired(stored, params.clockMillis())) {
        Timber.tag(LogTag.SYNC).i(
            "Sync checkpoint expired for %ss %s; resetting to full sync",
            params.entityLabel,
            params.logContext
        )
        params.checkpointDataSource.clearCheckpoint(params.groupId, params.collectionName)
        return null
    }
    return stored
}

private suspend fun <T> fetchOnceWithDeltaSync(
    effectiveWatermark: SyncWatermark?,
    params: DeltaSyncParams<T>
) {
    try {
        val batch = if (effectiveWatermark == null) {
            params.fetchFullSnapshot()
        } else {
            params.fetchDeltaOnce(effectiveWatermark)
        }
        if (params.verifyParentExists != null && !params.verifyParentExists.invoke()) {
            Timber.tag(LogTag.SYNC).w(
                "Parent missing, skipping one-shot delta reconciliation for %ss",
                params.entityLabel
            )
            return
        }
        params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_SUBSCRIBE_AND_RECONCILE) {
            applyBatchLocal(batch, params)
            maybeAdvanceWatermark(effectiveWatermark, batch, params)
            params.onAfterReconcile?.invoke()
            params.syncedArchivedGroupIds.add(params.groupId)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(e, "Error fetching archived %ss, using local cache", params.entityLabel)
    }
}

private data class BootstrapResult(
    val watermark: SyncWatermark?,
    val shouldContinue: Boolean
)

private suspend fun <T> bootstrapFullSnapshot(params: DeltaSyncParams<T>): BootstrapResult {
    return try {
        val fullBatch = params.fetchFullSnapshot()
        if (params.verifyParentExists != null && !params.verifyParentExists.invoke()) {
            Timber.tag(LogTag.SYNC).w(
                "Parent missing, skipping bootstrap reconciliation for %ss",
                params.entityLabel
            )
            return BootstrapResult(watermark = null, shouldContinue = false)
        }
        var updatedWatermark: SyncWatermark? = null
        params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_SUBSCRIBE_AND_RECONCILE) {
            params.replaceLocal(fullBatch.upserts)
            confirmPendingSync(
                params.getPendingIds,
                params.verifyOnServer,
                params.markSynced,
                params.entityLabel
            )
            params.onAfterReconcile?.invoke()
            updatedWatermark = maybeAdvanceWatermark(null, fullBatch, params)
        }
        BootstrapResult(watermark = updatedWatermark, shouldContinue = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(
            e,
            "Bootstrap full snapshot failed for %ss %s; falling back to delta listener",
            params.entityLabel,
            params.logContext
        )
        BootstrapResult(watermark = null, shouldContinue = true)
    }
}

private sealed interface DeltaWindowOutcome {
    data class Continue(val watermark: SyncWatermark?) : DeltaWindowOutcome
    data class AdvanceCursor(val watermark: SyncWatermark) : DeltaWindowOutcome
    data object StopListening : DeltaWindowOutcome
    data object Completed : DeltaWindowOutcome
}

private class AbortDeltaCollectionSignal : CancellationException("Stopping current delta listener window")

private suspend fun <T> runRealtimeDeltaLoop(
    initialWatermark: SyncWatermark?,
    params: DeltaSyncParams<T>
) {
    var currentWatermark = initialWatermark
    while (currentCoroutineContext().isActive) {
        when (val outcome = collectDeltaWindow(currentWatermark, params)) {
            is DeltaWindowOutcome.AdvanceCursor -> currentWatermark = outcome.watermark
            DeltaWindowOutcome.StopListening, DeltaWindowOutcome.Completed, is DeltaWindowOutcome.Continue -> break
        }
    }
}

private suspend fun <T> collectDeltaWindow(
    initialWatermark: SyncWatermark?,
    params: DeltaSyncParams<T>
): DeltaWindowOutcome {
    var currentWatermark = initialWatermark
    var outcome: DeltaWindowOutcome = DeltaWindowOutcome.Completed
    try {
        params.observeDeltaFlow(initialWatermark).collect { batch ->
            when (val stepOutcome = processDeltaBatch(currentWatermark, batch, params)) {
                is DeltaWindowOutcome.Continue -> {
                    currentWatermark = stepOutcome.watermark ?: currentWatermark
                }
                is DeltaWindowOutcome.AdvanceCursor, DeltaWindowOutcome.StopListening -> {
                    outcome = stepOutcome
                    throw AbortDeltaCollectionSignal()
                }
                DeltaWindowOutcome.Completed -> Unit
            }
        }
    } catch (_: AbortDeltaCollectionSignal) {
        // Outcome was already recorded before aborting collection.
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(
            e,
            "Error subscribing to cloud %s delta changes, using local cache",
            params.entityLabel
        )
    }
    return outcome
}

private suspend fun <T> processDeltaBatch(
    currentWatermark: SyncWatermark?,
    batch: DeltaSyncBatch<T>,
    params: DeltaSyncParams<T>
): DeltaWindowOutcome {
    if (params.verifyParentExists != null && !params.verifyParentExists.invoke()) {
        Timber.tag(LogTag.SYNC).w("Parent missing, stopping delta listener for %ss", params.entityLabel)
        return DeltaWindowOutcome.StopListening
    }
    val advancedWatermark = reconcileDeltaBatch(currentWatermark, batch, params)
    return if (batch.reachedPageLimit && batch.isServerConfirmed && advancedWatermark != null) {
        DeltaWindowOutcome.AdvanceCursor(advancedWatermark)
    } else {
        DeltaWindowOutcome.Continue(advancedWatermark)
    }
}

private suspend fun <T> reconcileDeltaBatch(
    currentWatermark: SyncWatermark?,
    batch: DeltaSyncBatch<T>,
    params: DeltaSyncParams<T>
): SyncWatermark? {
    return try {
        var advanced: SyncWatermark? = null
        params.performanceMonitor.traceAsync(PerformanceTraces.SYNC_SUBSCRIBE_AND_RECONCILE) {
            applyBatchLocal(batch, params)
            advanced = maybeAdvanceWatermark(currentWatermark, batch, params)
            confirmPendingSync(
                params.getPendingIds,
                params.verifyOnServer,
                params.markSynced,
                params.entityLabel
            )
            params.onAfterReconcile?.invoke()
        }
        advanced
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.tag(LogTag.SYNC).w(
            e,
            "Error reconciling %ss from delta batch",
            params.entityLabel
        )
        null
    }
}

private suspend fun <T> applyBatchLocal(
    batch: DeltaSyncBatch<T>,
    params: DeltaSyncParams<T>
) {
    if (batch.isFullSnapshot) {
        params.replaceLocal(batch.upserts)
    } else if (batch.upserts.isNotEmpty() || batch.deletedIds.isNotEmpty()) {
        params.applyDeltaLocal(batch.upserts, batch.deletedIds)
    }
}

private suspend fun <T> maybeAdvanceWatermark(
    currentWatermark: SyncWatermark?,
    batch: DeltaSyncBatch<T>,
    params: DeltaSyncParams<T>
): SyncWatermark? {
    val candidate = batch.maxServerUpdatedAt ?: return null
    if (!batch.isServerConfirmed) return null
    if (currentWatermark != null && candidate <= currentWatermark) return null

    params.checkpointDataSource.saveWatermark(
        groupId = params.groupId,
        collectionName = params.collectionName,
        watermark = candidate,
        syncedAtClientMillis = params.clockMillis()
    )
    return candidate
}
