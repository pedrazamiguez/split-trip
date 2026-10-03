package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertThrows
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
    private lateinit var authenticationService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor
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
        authenticationService = mockk()
        performanceMonitor = mockk(relaxed = true) {
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
        }

        coEvery { authenticationService.currentUserId() } returns testUserId

        repository = SettlementRepositoryImpl(
            cloudSettlementDataSource = cloudSettlementDataSource,
            localSettlementDataSource = localSettlementDataSource,
            performanceMonitor = performanceMonitor,
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
        fun `purges remote records with SUGGESTED status from cloud`() = runTest(testDispatcher) {
            val legacySuggested = testRecord.copy(id = "legacy-sug", status = SettlementStatus.SUGGESTED)
            val confirmedRecord = testRecord.copy(id = "confirmed-rec", status = SettlementStatus.CONFIRMED_BY_PAYER)

            every { localSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudSettlementDataSource.getSettlementsByGroupIdFlow(testGroupId) } returns flowOf(
                listOf(legacySuggested, confirmedRecord)
            )
            coEvery { localSettlementDataSource.getPendingSyncSettlementIds(testGroupId) } returns emptyList()
            coEvery { cloudSettlementDataSource.deleteSettlement(any(), any()) } returns Unit
            coEvery { localSettlementDataSource.replaceSettlementsForGroup(any(), any()) } returns Unit

            repository.getGroupSettlementsFlow(testGroupId).first()

            advanceUntilIdle()

            coVerify(exactly = 1) {
                cloudSettlementDataSource.deleteSettlement(testGroupId, "legacy-sug")
            }
            coVerify(exactly = 1) {
                localSettlementDataSource.replaceSettlementsForGroup(
                    testGroupId,
                    match { list ->
                        list.none { it.status == SettlementStatus.SUGGESTED } &&
                            list.contains(confirmedRecord)
                    }
                )
            }
        }
    }
}
