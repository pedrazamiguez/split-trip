package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.KeyedSubscriptionTracker
import es.pedrazamiguez.splittrip.data.sync.SyncReconciliationParams
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.sync.subscribeAndReconcile
import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.data.sync.syncDeletionToCloud
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSubunitDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSubunitDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class SubunitRepositoryImpl(
    private val cloudSubunitDataSource: CloudSubunitDataSource,
    private val localSubunitDataSource: LocalSubunitDataSource,
    private val localGroupDataSource: LocalGroupDataSource,
    private val authenticationService: AuthenticationService,
    private val performanceMonitor: PerformanceMonitor,
    private val syncTeardownCoordinator: SyncTeardownCoordinator,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SubunitRepository {

    private val syncScope = CoroutineScope(ioDispatcher)
    private val subscriptionTracker = KeyedSubscriptionTracker()

    init {
        syncTeardownCoordinator.registerAction { subscriptionTracker.cancelAll() }
    }

    override suspend fun createSubunit(groupId: String, subunit: Subunit): String {
        val subunitId = subunit.id.ifBlank { UUID.randomUUID().toString() }
        val currentUserId = authenticationService.currentUserId() ?: ""
        val currentTimestamp = LocalDateTime.now()

        val subunitWithMetadata = subunit.copy(
            id = subunitId,
            groupId = groupId,
            createdBy = subunit.createdBy.ifBlank { currentUserId },
            createdAt = subunit.createdAt ?: currentTimestamp,
            lastUpdatedAt = currentTimestamp,
            syncStatus = SyncStatus.PENDING_SYNC
        )

        localSubunitDataSource.saveSubunit(subunitWithMetadata)

        syncCreateToCloud(
            scope = syncScope,
            entityId = subunitWithMetadata.id,
            cloudWrite = { cloudSubunitDataSource.addSubunit(groupId, subunitWithMetadata) },
            updateSyncStatus = localSubunitDataSource::updateSyncStatus,
            getCurrentSyncStatus = { id ->
                localSubunitDataSource.getSubunitById(id)?.syncStatus ?: SyncStatus.PENDING_SYNC
            },
            entityLabel = ENTITY_LABEL,
            performanceMonitor = performanceMonitor
        )

        return subunitId
    }

    override suspend fun updateSubunit(groupId: String, subunit: Subunit) {
        val currentTimestamp = LocalDateTime.now()

        val updatedSubunit = subunit.copy(
            groupId = groupId,
            lastUpdatedAt = currentTimestamp,
            syncStatus = SyncStatus.PENDING_SYNC
        )

        localSubunitDataSource.saveSubunit(updatedSubunit)

        syncCreateToCloud(
            scope = syncScope,
            entityId = updatedSubunit.id,
            cloudWrite = { cloudSubunitDataSource.updateSubunit(groupId, updatedSubunit) },
            updateSyncStatus = localSubunitDataSource::updateSyncStatus,
            getCurrentSyncStatus = { id ->
                localSubunitDataSource.getSubunitById(id)?.syncStatus ?: SyncStatus.PENDING_SYNC
            },
            entityLabel = "$ENTITY_LABEL update",
            performanceMonitor = performanceMonitor
        )
    }

    override suspend fun deleteSubunit(groupId: String, subunitId: String) {
        localSubunitDataSource.deleteSubunit(subunitId)

        syncDeletionToCloud(
            scope = syncScope,
            entityId = subunitId,
            cloudDelete = { cloudSubunitDataSource.deleteSubunit(groupId, subunitId) },
            entityLabel = ENTITY_LABEL
        )
    }

    /**
     * Returns a Flow of subunits for a group from local storage.
     * On start, subscribes to real-time cloud changes for multi-user sync.
     *
     * Uses [KeyedSubscriptionTracker] to enforce a single shared subscription
     * per groupId, preventing duplicate snapshot listeners from accumulating
     * across flatMapLatest restarts, config changes, or WhileSubscribed
     * resubscriptions.
     */
    override fun getGroupSubunitsFlow(groupId: String): Flow<List<Subunit>> =
        localSubunitDataSource.getSubunitsByGroupIdFlow(groupId)
            .onStart {
                subscriptionTracker.cancelAndRelaunch(groupId, syncScope) {
                    subscribeAndReconcile(
                        cloudFlow = cloudSubunitDataSource.getSubunitsByGroupIdFlow(groupId),
                        params = SyncReconciliationParams(
                            reconcileLocal = { remoteSubunits ->
                                localSubunitDataSource.replaceSubunitsForGroup(groupId, remoteSubunits)
                            },
                            getPendingIds = { localSubunitDataSource.getPendingSyncSubunitIds(groupId) },
                            verifyOnServer = { id ->
                                cloudSubunitDataSource.verifySubunitOnServer(groupId, id)
                            },
                            markSynced = { id ->
                                localSubunitDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                            },
                            entityLabel = ENTITY_LABEL,
                            logContext = "for group $groupId",
                            performanceMonitor = performanceMonitor,
                            verifyParentExists = { localGroupDataSource.getGroupById(groupId) != null }
                        )
                    )
                }
            }

    override suspend fun getSubunitById(subunitId: String): Subunit? =
        localSubunitDataSource.getSubunitById(subunitId)

    override suspend fun getGroupSubunits(groupId: String): List<Subunit> =
        localSubunitDataSource.getSubunitsByGroupId(groupId)

    companion object {
        private const val ENTITY_LABEL = "subunit"
    }
}
