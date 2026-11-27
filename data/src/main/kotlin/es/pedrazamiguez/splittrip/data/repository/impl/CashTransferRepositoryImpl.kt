package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.local.dao.CashTransferDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntity
import es.pedrazamiguez.splittrip.data.sync.DeltaSyncParams
import es.pedrazamiguez.splittrip.data.sync.KeyedSubscriptionTracker
import es.pedrazamiguez.splittrip.data.sync.SyncCreateParams
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.data.sync.syncGroupCollectionDelta
import es.pedrazamiguez.splittrip.data.sync.trackSubscription
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashTransferDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CashTransferRepositoryImpl(
    private val cashTransferDao: CashTransferDao,
    private val cloudCashTransferDataSource: CloudCashTransferDataSource,
    private val localGroupDataSource: LocalGroupDataSource,
    private val localSyncCheckpointDataSource: LocalSyncCheckpointDataSource,
    private val performanceMonitor: PerformanceMonitor,
    private val syncTeardownCoordinator: SyncTeardownCoordinator,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CashTransferRepository {

    private val syncScope = CoroutineScope(ioDispatcher)
    private val subscriptionTracker = KeyedSubscriptionTracker()
    private val syncedArchivedGroupIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        syncTeardownCoordinator.registerAction {
            subscriptionTracker.cancelAll()
            syncedArchivedGroupIds.clear()
        }
    }

    override suspend fun addTransfer(transfer: CashTransfer): Result<Unit> {
        val transferWithMetadata = if (transfer.id.isBlank()) {
            transfer.copy(id = UUID.randomUUID().toString())
        } else {
            transfer
        }

        // Save to local first
        cashTransferDao.insert(transferWithMetadata.toEntity(SyncStatus.PENDING_SYNC))

        // Sync to cloud in background
        syncCreateToCloud(
            scope = syncScope,
            entityId = transferWithMetadata.id,
            params = SyncCreateParams(
                cloudWrite = { cloudCashTransferDataSource.upsertCashTransfer(transferWithMetadata).getOrThrow() },
                updateSyncStatus = { id, status -> cashTransferDao.updateSyncStatus(id, status.name) },
                getCurrentSyncStatus = { id ->
                    cashTransferDao.getCashTransferById(id)?.syncStatus ?: SyncStatus.PENDING_SYNC
                },
                entityLabel = "cash_transfer",
                performanceMonitor = performanceMonitor
            )
        )

        return Result.success(Unit)
    }

    override fun observeGroupCashTransfers(groupId: String): Flow<List<CashTransfer>> {
        return cashTransferDao.observeByGroupId(groupId)
            .map { entities -> entities.map { it.toDomain() } }
            .trackSubscription(subscriptionTracker, groupId, syncScope) {
                syncGroupCollectionDelta(
                    DeltaSyncParams(
                        groupId = groupId,
                        collectionName = SyncCollectionName.CASH_TRANSFERS,
                        checkpointDataSource = localSyncCheckpointDataSource,
                        isGroupArchived = {
                            localGroupDataSource.getGroupById(groupId)?.status == GroupStatus.ARCHIVED
                        },
                        syncedArchivedGroupIds = syncedArchivedGroupIds,
                        fetchFullSnapshot = {
                            cloudCashTransferDataSource.fetchGroupCashTransfersFullSnapshot(groupId)
                        },
                        fetchDeltaOnce = { watermark ->
                            cloudCashTransferDataSource.fetchGroupCashTransfersDelta(groupId, watermark)
                        },
                        observeDeltaFlow = { watermark ->
                            cloudCashTransferDataSource.observeGroupCashTransfersDeltaFlow(groupId, watermark)
                        },
                        replaceLocal = { remoteTransfers ->
                            cashTransferDao.replaceCashTransfersForGroup(
                                groupId,
                                remoteTransfers.map { it.toEntity(SyncStatus.SYNCED) }
                            )
                        },
                        applyDeltaLocal = { upserts, deletedIds ->
                            cashTransferDao.applyCashTransfersDeltaForGroup(
                                groupId,
                                upserts.map { it.toEntity(SyncStatus.SYNCED) },
                                deletedIds
                            )
                        },
                        getPendingIds = { cashTransferDao.getPendingSyncCashTransferIds(groupId) },
                        verifyOnServer = { id ->
                            cloudCashTransferDataSource.verifyCashTransferOnServer(groupId, id)
                        },
                        markSynced = { id ->
                            cashTransferDao.updateSyncStatus(id, SyncStatus.SYNCED.name)
                        },
                        entityLabel = "cash_transfer",
                        logContext = "for group $groupId",
                        performanceMonitor = performanceMonitor,
                        verifyParentExists = { localGroupDataSource.getGroupById(groupId) != null }
                    )
                )
            }
    }
}
