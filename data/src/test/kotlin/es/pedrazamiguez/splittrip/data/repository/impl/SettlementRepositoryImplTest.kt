package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SettlementRepositoryImpl")
class SettlementRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cloudSettlementDataSource: CloudSettlementDataSource
    private lateinit var localSettlementDataSource: LocalSettlementDataSource
    private lateinit var localGroupDataSource: LocalGroupDataSource
    private lateinit var localSyncCheckpointDataSource: LocalSyncCheckpointDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var syncTeardownCoordinator: SyncTeardownCoordinator
    private lateinit var repository: SettlementRepositoryImpl

    private val testGroupId = "group-123"
    private val testUserId = "user-1"
    private val testTargetUserId = "user-2"

    private val testSettlement = Settlement(
        fromUserId = testUserId,
        toUserId = testTargetUserId,
        amount = 1500L,
        currency = "EUR",
        sourcePocket = SettlementPocketType.CASH
    )

    private val testRecord = SettlementRecord(
        id = "settlement-1",
        groupId = testGroupId,
        settlement = testSettlement,
        status = SettlementStatus.SUGGESTED,
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
    )

    @BeforeEach
    fun setUp() {
        cloudSettlementDataSource = mockk(relaxed = true)
        localSettlementDataSource = mockk(relaxed = true)
        localGroupDataSource = mockk(relaxed = true)
        localSyncCheckpointDataSource = mockk(relaxed = true)
        coEvery { localSyncCheckpointDataSource.getWatermark(any(), any()) } returns null
        authenticationService = mockk()
        performanceMonitor = mockk(relaxed = true) {
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
        }
        syncTeardownCoordinator = SyncTeardownCoordinator()

        coEvery { authenticationService.currentUserId() } returns testUserId

        repository = SettlementRepositoryImpl(
            cloudSettlementDataSource = cloudSettlementDataSource,
            localSettlementDataSource = localSettlementDataSource,
            localGroupDataSource = localGroupDataSource,
            localSyncCheckpointDataSource = localSyncCheckpointDataSource,
            performanceMonitor = performanceMonitor,
            syncTeardownCoordinator = syncTeardownCoordinator,
            ioDispatcher = testDispatcher
        )
    }

    @Nested
    @DisplayName("AddSettlement")
    inner class AddSettlement {

        @Test
        fun `saves to local storage first and triggers background cloud sync`() = runTest(testDispatcher) {
            coEvery { localSettlementDataSource.saveSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.getSyncStatus(testRecord.id) } returns SyncStatus.PENDING_SYNC
            coEvery { cloudSettlementDataSource.upsertSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.updateSyncStatus(any(), any()) } returns Unit

            repository.addSettlement(testRecord)

            coVerify(exactly = 1) {
                localSettlementDataSource.saveSettlement(testRecord, SyncStatus.PENDING_SYNC)
            }

            advanceUntilIdle()

            coVerify(exactly = 1) {
                cloudSettlementDataSource.upsertSettlement(testGroupId, testRecord)
                localSettlementDataSource.updateSyncStatus(testRecord.id, SyncStatus.SYNCED)
            }
        }

        @Test
        fun `throws IllegalArgumentException when fromUserId is blank`() = runTest(testDispatcher) {
            val invalidRecord = testRecord.copy(
                settlement = testSettlement.copy(fromUserId = "")
            )

            assertThrows(IllegalArgumentException::class.java) {
                runTest(testDispatcher) {
                    repository.addSettlement(invalidRecord)
                }
            }

            coVerify(exactly = 0) {
                localSettlementDataSource.saveSettlement(any(), any())
            }
        }

        @Test
        fun `throws IllegalArgumentException when toUserId is blank`() = runTest(testDispatcher) {
            val invalidRecord = testRecord.copy(
                settlement = testSettlement.copy(toUserId = " ")
            )

            assertThrows(IllegalArgumentException::class.java) {
                runTest(testDispatcher) {
                    repository.addSettlement(invalidRecord)
                }
            }

            coVerify(exactly = 0) {
                localSettlementDataSource.saveSettlement(any(), any())
            }
        }
    }

    @Nested
    @DisplayName("UpdateSettlement")
    inner class UpdateSettlement {

        @Test
        fun `saves locally as pending and updates cloud`() = runTest(testDispatcher) {
            coEvery { localSettlementDataSource.saveSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.getSyncStatus(testRecord.id) } returns SyncStatus.PENDING_SYNC
            coEvery { cloudSettlementDataSource.upsertSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.updateSyncStatus(any(), any()) } returns Unit

            repository.updateSettlement(testRecord)

            coVerify(exactly = 1) {
                localSettlementDataSource.saveSettlement(testRecord, SyncStatus.PENDING_SYNC)
            }

            advanceUntilIdle()

            coVerify(exactly = 1) {
                cloudSettlementDataSource.upsertSettlement(testGroupId, testRecord)
                localSettlementDataSource.updateSyncStatus(testRecord.id, SyncStatus.SYNCED)
            }
        }
    }

    @Nested
    @DisplayName("DeleteSettlement")
    inner class DeleteSettlement {

        @Test
        fun `deletes locally first and triggers background cloud deletion`() = runTest(testDispatcher) {
            coEvery { localSettlementDataSource.deleteSettlement(testRecord.id) } returns Unit
            coEvery { cloudSettlementDataSource.deleteSettlement(any(), any()) } returns Unit

            repository.deleteSettlement(testRecord)

            coVerify(exactly = 1) {
                localSettlementDataSource.deleteSettlement(testRecord.id)
            }

            advanceUntilIdle()

            coVerify(exactly = 1) {
                cloudSettlementDataSource.deleteSettlement(testGroupId, testRecord.id)
            }
        }
    }

    @Nested
    @DisplayName("GetGroupSettlementsFlow")
    inner class GetGroupSettlementsFlow {

        @Test
        fun `purges remote records with SUGGESTED status from cloud inside delta batch`() = runTest(testDispatcher) {
            val legacySuggested = testRecord.copy(id = "legacy-sug", status = SettlementStatus.SUGGESTED)
            val confirmedRecord = testRecord.copy(id = "confirmed-rec", status = SettlementStatus.CONFIRMED_BY_PAYER)
            val validWatermark = SyncWatermark((System.currentTimeMillis() / 1000L) - 60L, 0)

            every { localSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            coEvery {
                localSyncCheckpointDataSource.getWatermark(testGroupId, SyncCollectionName.SETTLEMENTS)
            } returns validWatermark
            every { cloudSettlementDataSource.observeSettlementsDeltaFlow(testGroupId, validWatermark) } returns flowOf(
                DeltaSyncBatch(
                    upserts = listOf(legacySuggested, confirmedRecord),
                    deletedIds = listOf("deleted-1"),
                    maxServerUpdatedAt = SyncWatermark(System.currentTimeMillis() / 1000L, 0),
                    isServerConfirmed = true,
                    isFullSnapshot = false
                )
            )
            coEvery { localSettlementDataSource.getPendingSyncSettlementIds(testGroupId) } returns emptyList()
            coEvery { cloudSettlementDataSource.deleteSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.applySettlementsDeltaForGroup(any(), any(), any()) } returns Unit

            repository.getGroupSettlementsFlow(testGroupId).first()

            advanceUntilIdle()

            coVerify(exactly = 0) { cloudSettlementDataSource.fetchSettlementsFullSnapshot(any()) }
            coVerify(exactly = 1) {
                cloudSettlementDataSource.deleteSettlement(testGroupId, "legacy-sug")
            }
            coVerify(exactly = 1) {
                localSettlementDataSource.applySettlementsDeltaForGroup(
                    testGroupId,
                    match { list ->
                        list.none { it.status == SettlementStatus.SUGGESTED } &&
                            list.contains(confirmedRecord)
                    },
                    listOf("deleted-1")
                )
            }
        }

        @Test
        fun `registers teardown callback and cancels active listener on sign-out`() = runTest(testDispatcher) {
            val localFlow = MutableSharedFlow<List<SettlementRecord>>()
            val validWatermark = SyncWatermark((System.currentTimeMillis() / 1000L) - 60L, 0)
            var cancelled = false
            every { localSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns localFlow
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns Group(
                id = testGroupId,
                status = GroupStatus.ACTIVE
            )
            coEvery {
                localSyncCheckpointDataSource.getWatermark(testGroupId, SyncCollectionName.SETTLEMENTS)
            } returns validWatermark
            every { cloudSettlementDataSource.observeSettlementsDeltaFlow(testGroupId, validWatermark) } returns flow {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }

            val collectorJob = launch { repository.getGroupSettlementsFlow(testGroupId).collect {} }
            runCurrent()
            assertFalse(cancelled)

            syncTeardownCoordinator.teardownAll()
            runCurrent()

            assertTrue(cancelled)
            collectorJob.cancel()
            advanceUntilIdle()
        }

        @Test
        fun `uses one-shot fetch without persistent listener for ARCHIVED group`() = runTest(testDispatcher) {
            val confirmedRecord = testRecord.copy(id = "confirmed-rec", status = SettlementStatus.CONFIRMED_BY_PAYER)
            every { localSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns Group(
                id = testGroupId,
                status = GroupStatus.ARCHIVED
            )
            coEvery { cloudSettlementDataSource.fetchSettlementsFullSnapshot(testGroupId) } returns DeltaSyncBatch(
                upserts = listOf(confirmedRecord),
                deletedIds = emptyList(),
                maxServerUpdatedAt = SyncWatermark(100L, 0),
                isServerConfirmed = true,
                isFullSnapshot = true
            )
            coEvery { localSettlementDataSource.replaceSettlementsForGroup(any(), any()) } returns Unit

            repository.getGroupSettlementsFlow(testGroupId).first()
            advanceUntilIdle()
            repository.getGroupSettlementsFlow(testGroupId).first()
            advanceUntilIdle()

            verify(exactly = 0) { cloudSettlementDataSource.observeSettlementsDeltaFlow(testGroupId, any()) }
            coVerify(exactly = 1) { cloudSettlementDataSource.fetchSettlementsFullSnapshot(testGroupId) }
            coVerify(exactly = 1) {
                localSettlementDataSource.replaceSettlementsForGroup(testGroupId, listOf(confirmedRecord))
            }

            syncTeardownCoordinator.teardownAll()
            repository.getGroupSettlementsFlow(testGroupId).first()
            advanceUntilIdle()

            coVerify(exactly = 2) { cloudSettlementDataSource.fetchSettlementsFullSnapshot(testGroupId) }
        }

        @Test
        fun `expired checkpoint falls back to fetchSettlementsFullSnapshot and replaceSettlementsForGroup`() =
            runTest(testDispatcher) {
                val expired = SyncWatermark(1000L, 0)
                val confirmedRecord = testRecord.copy(
                    id = "confirmed-rec",
                    status = SettlementStatus.CONFIRMED_BY_PAYER
                )
                every { localSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
                coEvery {
                    localSyncCheckpointDataSource.getWatermark(testGroupId, SyncCollectionName.SETTLEMENTS)
                } returns expired
                coEvery { cloudSettlementDataSource.fetchSettlementsFullSnapshot(testGroupId) } returns DeltaSyncBatch(
                    upserts = listOf(confirmedRecord),
                    deletedIds = emptyList(),
                    maxServerUpdatedAt = SyncWatermark(System.currentTimeMillis() / 1000L, 0),
                    isServerConfirmed = true,
                    isFullSnapshot = true
                )
                every { cloudSettlementDataSource.observeSettlementsDeltaFlow(testGroupId, any()) } returns
                    kotlinx.coroutines.flow.emptyFlow()

                repository.getGroupSettlementsFlow(testGroupId).first()
                advanceUntilIdle()

                coVerify(exactly = 1) { cloudSettlementDataSource.fetchSettlementsFullSnapshot(testGroupId) }
                coVerify(exactly = 1) {
                    localSettlementDataSource.replaceSettlementsForGroup(testGroupId, listOf(confirmedRecord))
                }
            }
    }
}
