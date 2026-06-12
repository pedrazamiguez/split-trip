package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

class ContributionRepositoryImpl(
    private val cloudContributionDataSource: CloudContributionDataSource,
    private val localContributionDataSource: LocalContributionDataSource,
    private val authenticationService: AuthenticationService,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ContributionRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    /**
     * Tracks active cloud subscription Jobs per groupId.
     * Prevents duplicate Firestore snapshot listeners from accumulating
     * when onStart fires multiple times (e.g., config changes, tab switches,
     * WhileSubscribed resubscriptions, flatMapLatest restarts).
     */
    private val cloudSubscriptionJobs = ConcurrentHashMap<String, Job>()

    override suspend fun addContribution(groupId: String, contribution: Contribution) {
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

    /**
     * Returns a Flow of contributions for a group from local storage.
     * On start, subscribes to real-time cloud changes for multi-user sync.
     *
     * Uses a single shared subscription per groupId: any existing cloud listener
     * for this group is cancelled before starting a new one, preventing duplicate
     * snapshot listeners from accumulating across flatMapLatest restarts,
     * config changes, or WhileSubscribed resubscriptions.
     */
    override fun getGroupContributionsFlow(groupId: String): Flow<List<Contribution>> =
        localContributionDataSource.getContributionsByGroupIdFlow(groupId)
            .onStart {
                // Cancel any previous cloud subscription for this group to prevent duplicates.
                cloudSubscriptionJobs[groupId]?.cancel()
                cloudSubscriptionJobs[groupId] = syncScope.launch {
                    subscribeToCloudChanges(groupId)
                }
            }

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

    /**
     * Subscribes to real-time Firestore snapshot changes for a group's contributions.
     *
     * The Firestore snapshotListener fires whenever ANY user adds, modifies, or
     * deletes a contribution in this group. Each snapshot represents the complete
     * authoritative state of the collection.
     *
     * We use [replaceContributionsForGroup] with a merge reconciliation strategy
     * (upsert remote + selective delete of stale) to safely reconcile the
     * local DB with the cloud snapshot.
     *
     * After reconciliation, [confirmPendingSyncContributions] attempts to verify
     * any PENDING_SYNC items against the server. This handles the
     * PENDING_SYNC → SYNCED transition when the device comes back online
     * after an app restart.
     */
    private suspend fun subscribeToCloudChanges(groupId: String) {
        try {
            cloudContributionDataSource.getContributionsByGroupIdFlow(groupId)
                .collect { remoteContributions ->
                    try {
                        Timber.d("Real-time sync: ${remoteContributions.size} contributions for group $groupId")
                        localContributionDataSource.replaceContributionsForGroup(
                            groupId,
                            remoteContributions
                        )
                        confirmPendingSyncContributions(groupId)
                    } catch (e: Exception) {
                        Timber.w(e, "Error reconciling contributions from cloud snapshot")
                    }
                }
        } catch (e: Exception) {
            Timber.w(e, "Error subscribing to cloud contribution changes, using local cache")
        }
    }

    /**
     * Attempts to confirm PENDING_SYNC contributions by verifying their existence on the server.
     *
     * Called after each reconciliation cycle. When the device is online and Firestore
     * has confirmed the pending write, the server verification succeeds and the
     * contribution transitions to SYNCED. When offline, the verification throws and the
     * contribution remains PENDING_SYNC.
     */
    private suspend fun confirmPendingSyncContributions(groupId: String) {
        val pendingIds = localContributionDataSource.getPendingSyncContributionIds(groupId)
        if (pendingIds.isEmpty()) return

        for (id in pendingIds) {
            try {
                if (cloudContributionDataSource.verifyContributionOnServer(groupId, id)) {
                    localContributionDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                    Timber.d("Confirmed contribution sync: $id")
                }
            } catch (e: Exception) {
                Timber.d(e, "Cannot confirm contribution $id — server unreachable")
            }
        }
    }
}
