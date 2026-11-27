package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.KeyedSubscriptionTracker
import es.pedrazamiguez.splittrip.data.sync.SyncCreateParams
import es.pedrazamiguez.splittrip.data.sync.SyncReconciliationParams
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.data.sync.syncDeletionToCloud
import es.pedrazamiguez.splittrip.data.sync.syncGroupCollection
import es.pedrazamiguez.splittrip.data.sync.trackSubscription
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class SettlementRepositoryImpl(
    private val cloudSettlementDataSource: CloudSettlementDataSource,
    private val localSettlementDataSource: LocalSettlementDataSource,
    private val localGroupDataSource: LocalGroupDataSource,
    private val performanceMonitor: PerformanceMonitor,
    private val syncTeardownCoordinator: SyncTeardownCoordinator,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettlementRepository {

    private val syncScope = CoroutineScope(ioDispatcher)
    private val subscriptionTracker = KeyedSubscriptionTracker()
    private val syncedArchivedGroupIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        syncTeardownCoordinator.registerAction {
            subscriptionTracker.cancelAll()
            syncedArchivedGroupIds.clear()
        }
    }

    override fun getGroupSettlementsFlow(groupId: String): Flow<List<SettlementRecord>> =
        localSettlementDataSource.getSettlementsByGroupIdFlow(groupId)
            .trackSubscription(subscriptionTracker, groupId, syncScope) {
                syncGroupCollection(
                    groupId = groupId,
                    isGroupArchived = {
                        localGroupDataSource.getGroupById(groupId)?.status == GroupStatus.ARCHIVED
                    },
                    syncedArchivedGroupIds = syncedArchivedGroupIds,
                    fetchOnce = { cloudSettlementDataSource.fetchSettlementsByGroupId(groupId) },
                    cloudFlow = { cloudSettlementDataSource.getSettlementsByGroupIdFlow(groupId) },
                    params = SyncReconciliationParams(
                        reconcileLocal = { remoteRecords ->
                            val legacySuggested = remoteRecords.filter { it.status == SettlementStatus.SUGGESTED }
                            if (legacySuggested.isNotEmpty()) {
                                syncScope.launch {
                                    legacySuggested.forEach { stale ->
                                        try {
                                            cloudSettlementDataSource.deleteSettlement(groupId, stale.id)
                                        } catch (e: Exception) {
                                            Timber.w(e, "Failed to cleanup legacy suggested settlement ${stale.id}")
                                        }
                                    }
                                }
                            }
                            val validRecords = remoteRecords.filter { it.status != SettlementStatus.SUGGESTED }
                            localSettlementDataSource.replaceSettlementsForGroup(groupId, validRecords)
                        },
                        getPendingIds = {
                            localSettlementDataSource.getPendingSyncSettlementIds(groupId)
                        },
                        verifyOnServer = { id ->
                            cloudSettlementDataSource.verifySettlementOnServer(groupId, id)
                        },
                        markSynced = { id ->
                            localSettlementDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                        },
                        entityLabel = ENTITY_LABEL,
                        logContext = "for group $groupId",
                        performanceMonitor = performanceMonitor,
                        verifyParentExists = { localGroupDataSource.getGroupById(groupId) != null }
                    )
                )
            }

    override suspend fun getGroupSettlements(groupId: String): List<SettlementRecord> =
        localSettlementDataSource.getSettlementsByGroupId(groupId)

    override suspend fun getMemberSettlements(
        groupId: String,
        userId: String
    ): List<SettlementRecord> =
        localSettlementDataSource.getSettlementsByMember(groupId, userId)

    override suspend fun getSettlementById(id: String): SettlementRecord? =
        localSettlementDataSource.getSettlementById(id)

    override suspend fun addSettlement(record: SettlementRecord) {
        require(record.settlement.fromUserId.isNotBlank()) { "Settlement fromUserId cannot be blank" }
        require(record.settlement.toUserId.isNotBlank()) { "Settlement toUserId cannot be blank" }

        localSettlementDataSource.saveSettlement(record, SyncStatus.PENDING_SYNC)

        syncCreateToCloud(
            scope = syncScope,
            entityId = record.id,
            params = SyncCreateParams(
                cloudWrite = {
                    cloudSettlementDataSource.upsertSettlement(
                        record.groupId,
                        record
                    )
                },
                updateSyncStatus = localSettlementDataSource::updateSyncStatus,
                getCurrentSyncStatus = { id ->
                    localSettlementDataSource.getSyncStatus(id) ?: SyncStatus.PENDING_SYNC
                },
                entityLabel = ENTITY_LABEL,
                performanceMonitor = performanceMonitor
            )
        )
    }

    override suspend fun updateSettlement(record: SettlementRecord) {
        localSettlementDataSource.saveSettlement(record, SyncStatus.PENDING_SYNC)

        syncCreateToCloud(
            scope = syncScope,
            entityId = record.id,
            params = SyncCreateParams(
                cloudWrite = {
                    cloudSettlementDataSource.upsertSettlement(
                        record.groupId,
                        record
                    )
                },
                updateSyncStatus = localSettlementDataSource::updateSyncStatus,
                getCurrentSyncStatus = { id ->
                    localSettlementDataSource.getSyncStatus(id) ?: SyncStatus.PENDING_SYNC
                },
                entityLabel = "$ENTITY_LABEL update",
                performanceMonitor = performanceMonitor
            )
        )
    }

    override suspend fun deleteSettlement(record: SettlementRecord) {
        localSettlementDataSource.deleteSettlement(record.id)

        syncDeletionToCloud(
            scope = syncScope,
            entityId = record.id,
            cloudDelete = {
                cloudSettlementDataSource.deleteSettlement(record.groupId, record.id)
            },
            entityLabel = ENTITY_LABEL
        )
    }

    companion object {
        private const val ENTITY_LABEL = "settlement"
    }
}
