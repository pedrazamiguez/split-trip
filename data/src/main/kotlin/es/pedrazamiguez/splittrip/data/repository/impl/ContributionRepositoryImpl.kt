package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.core.performance.PerformanceTraces
import es.pedrazamiguez.splittrip.data.sync.KeyedSubscriptionTracker
import es.pedrazamiguez.splittrip.data.sync.SyncReconciliationParams
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.sync.subscribeAndReconcile
import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.LocalDateTime
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

class ContributionRepositoryImpl(
    private val cloudContributionDataSource: CloudContributionDataSource,
    private val localContributionDataSource: LocalContributionDataSource,
    private val localGroupDataSource: LocalGroupDataSource,
    private val authenticationService: AuthenticationService,
    private val performanceMonitor: PerformanceMonitor,
    private val syncTeardownCoordinator: SyncTeardownCoordinator,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ContributionRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    private val cloudSubscriptionTracker = KeyedSubscriptionTracker()

    init {
        syncTeardownCoordinator.registerAction { cloudSubscriptionTracker.cancelAll() }
    }

    override suspend fun addContribution(groupId: String, contribution: Contribution) {
        performanceMonitor.traceAsync(PerformanceTraces.CONTRIBUTION_ADD) {
            val contributionId = contribution.id.ifBlank { UUID.randomUUID().toString() }
            val currentUserId = authenticationService.currentUserId() ?: ""
            val currentTimestamp = LocalDateTime.now()

            val contributionWithMetadata = contribution.copy(
                id = contributionId,
                groupId = groupId,
                userId = contribution.userId.ifBlank { currentUserId },
                createdBy = currentUserId,
                createdAt = contribution.createdAt ?: currentTimestamp,
                lastUpdatedAt = currentTimestamp,
                syncStatus = SyncStatus.PENDING_SYNC
            )

            // Save to local first - UI updates instantly via Flow
            localContributionDataSource.saveContribution(contributionWithMetadata)

            // Sync to cloud in background
            syncScope.launch {
                try {
                    cloudContributionDataSource.addContribution(groupId, contributionWithMetadata)
                    localContributionDataSource.updateSyncStatus(contributionWithMetadata.id, SyncStatus.SYNCED)
                    Timber.d("Contribution synced to cloud: ${contributionWithMetadata.id}")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Only downgrade to SYNC_FAILED if the snapshot listener has not already
                    // confirmed the entity as SYNCED (guards against the ACK-loss race condition).
                    val currentStatus = localContributionDataSource
                        .findContributionById(contributionWithMetadata.id)?.syncStatus
                    if (currentStatus == SyncStatus.PENDING_SYNC) {
                        localContributionDataSource.updateSyncStatus(
                            contributionWithMetadata.id,
                            SyncStatus.SYNC_FAILED
                        )
                    }
                    Timber.w(e, "Failed to sync contribution to cloud")
                }
            }
        }
    }

    override suspend fun getContribution(contributionId: String): Contribution? =
        localContributionDataSource.findContributionById(contributionId)

    override suspend fun updateContribution(groupId: String, contribution: Contribution) {
        performanceMonitor.traceAsync(PerformanceTraces.CONTRIBUTION_UPDATE) {
            val contributionWithMetadata = contribution.copy(
                lastUpdatedAt = LocalDateTime.now(),
                syncStatus = SyncStatus.PENDING_SYNC
            )
            localContributionDataSource.saveContribution(contributionWithMetadata)
            syncCreateToCloud(
                scope = syncScope,
                entityId = contributionWithMetadata.id,
                entityLabel = "Contribution",
                cloudWrite = { cloudContributionDataSource.addContribution(groupId, contributionWithMetadata) },
                updateSyncStatus = { id, status -> localContributionDataSource.updateSyncStatus(id, status) },
                getCurrentSyncStatus = { id ->
                    localContributionDataSource.findContributionById(id)?.syncStatus ?: SyncStatus.SYNCED
                },
                performanceMonitor = performanceMonitor
            )
        }
    }

    /**
     * Returns a Flow of contributions for a group from local storage.
     * On start, subscribes to real-time cloud changes for multi-user sync.
     *
     * Uses a single shared subscription per groupId: any existing cloud listener
     * for this group is cancelled before starting a new one, preventing duplicate
     * snapshot listeners from accumulating across flatMapLatest restarts,
     * config changes, or WhileSubscribed resubscriptions.
     */
    override suspend fun deleteContribution(groupId: String, contributionId: String) {
        // Delete from local first - UI updates instantly via Flow
        localContributionDataSource.deleteContribution(contributionId)

        // Always queue cloud deletion, even for PENDING_SYNC entities.
        // Firestore SDK guarantees write ordering: the queued SET (from addContribution)
        // executes before this DELETE when connectivity is restored.
        syncScope.launch {
            try {
                cloudContributionDataSource.deleteContribution(groupId, contributionId)
                Timber.d("Contribution deletion synced to cloud: $contributionId")
            } catch (e: Exception) {
                Timber.w(e, "Failed to sync contribution deletion to cloud, will retry later")
            }
        }
    }

    override suspend fun deleteByLinkedExpenseId(groupId: String, linkedExpenseId: String) {
        // The domain model guarantees a 1:1 relationship between an expense and its
        // paired contribution. The local find retrieves the single expected contribution
        // ID for cloud sync, while the DAO DELETE cleans up by (groupId, linkedExpenseId).
        // In the unlikely event of duplicates (e.g., retry race), the Firestore snapshot
        // listener's merge reconciliation will remove any stale cloud documents on the
        // next sync cycle, so the system self-heals.
        val linkedContribution = localContributionDataSource.findByLinkedExpenseId(
            groupId,
            linkedExpenseId
        )

        // Delete from local first - UI updates instantly via Flow
        localContributionDataSource.deleteByLinkedExpenseId(groupId, linkedExpenseId)

        // Sync deletion to cloud in background (only if we found a contribution to delete)
        linkedContribution?.let { contribution ->
            syncScope.launch {
                try {
                    cloudContributionDataSource.deleteContribution(groupId, contribution.id)
                    Timber.d("Linked contribution deletion synced to cloud: ${contribution.id}")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to sync linked contribution deletion to cloud")
                }
            }
        }
    }

    override suspend fun findByLinkedExpenseId(
        groupId: String,
        linkedExpenseId: String
    ): Contribution? = localContributionDataSource.findByLinkedExpenseId(groupId, linkedExpenseId)

    override fun getGroupContributionsFlow(groupId: String): Flow<List<Contribution>> =
        localContributionDataSource.getContributionsByGroupIdFlow(groupId)
            .onStart {
                cloudSubscriptionTracker.cancelAndRelaunch(groupId, syncScope) {
                    subscribeAndReconcile(
                        cloudFlow = cloudContributionDataSource
                            .getContributionsByGroupIdFlow(groupId),
                        params = SyncReconciliationParams(
                            reconcileLocal = { remoteContributions ->
                                localContributionDataSource.replaceContributionsForGroup(
                                    groupId,
                                    remoteContributions
                                )
                            },
                            getPendingIds = {
                                localContributionDataSource.getPendingSyncContributionIds(groupId)
                            },
                            verifyOnServer = { id ->
                                cloudContributionDataSource.verifyContributionOnServer(groupId, id)
                            },
                            markSynced = { id ->
                                localContributionDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                            },
                            entityLabel = "contribution",
                            logContext = "for group $groupId",
                            performanceMonitor = performanceMonitor,
                            verifyParentExists = { localGroupDataSource.getGroupById(groupId) != null }
                        )
                    )
                }
            }
}
