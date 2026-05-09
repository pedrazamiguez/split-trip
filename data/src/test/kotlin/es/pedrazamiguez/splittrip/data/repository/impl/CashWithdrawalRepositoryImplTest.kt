package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalQueryDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalWriteDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashWithdrawalRepositoryImplTest {

    private lateinit var cloudDataSource: CloudCashWithdrawalDataSource
    private lateinit var localQueryDataSource: LocalCashWithdrawalQueryDataSource
    private lateinit var localWriteDataSource: LocalCashWithdrawalWriteDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: CashWithdrawalRepositoryImpl

    private val testUserId = "user-123"
    private val testGroupId = "group-123"
    private val testWithdrawal = CashWithdrawal(
        id = "w-1",
        groupId = testGroupId,
        withdrawnBy = "user-1",
        amountWithdrawn = 1000000L,
        remainingAmount = 1000000L,
        currency = "THB",
        deductedBaseAmount = 27000L,
        exchangeRate = java.math.BigDecimal("37.037"),
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
    )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        cloudDataSource = mockk(relaxed = true)
        localQueryDataSource = mockk(relaxed = true)
        localWriteDataSource = mockk(relaxed = true)
        authenticationService = mockk()
        every { authenticationService.currentUserId() } returns testUserId

        repository = CashWithdrawalRepositoryImpl(
            cloudCashWithdrawalDataSource = cloudDataSource,
            localQueryDataSource = localQueryDataSource,
            localWriteDataSource = localWriteDataSource,
            authenticationService = authenticationService,
            ioDispatcher = testDispatcher
        )
    }

    @Nested
    inner class AddWithdrawal {

        @Test
        fun `saves to local first before cloud sync`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs
            coEvery { cloudDataSource.addWithdrawal(any(), any()) } just Runs

            // When
            repository.addWithdrawal(testGroupId, testWithdrawal)
            advanceUntilIdle()

            // Then
            coVerifyOrder {
                localWriteDataSource.saveWithdrawal(any())
                cloudDataSource.addWithdrawal(testGroupId, any())
            }
        }

        @Test
        fun `generates UUID when id is blank`() = runTest(testDispatcher) {
            // Given
            val withdrawalNoId = testWithdrawal.copy(id = "")
            val slot = slot<CashWithdrawal>()
            coEvery { localWriteDataSource.saveWithdrawal(capture(slot)) } just Runs

            // When
            repository.addWithdrawal(testGroupId, withdrawalNoId)

            // Then
            assertTrue(slot.captured.id.isNotBlank())
        }

        @Test
        fun `sets createdBy to current authenticated user`() = runTest(testDispatcher) {
            // Given — withdrawal has a different withdrawnBy (impersonation scenario)
            val withdrawal = testWithdrawal.copy(withdrawnBy = "target-user")
            val slot = slot<CashWithdrawal>()
            coEvery { localWriteDataSource.saveWithdrawal(capture(slot)) } just Runs

            // When
            repository.addWithdrawal(testGroupId, withdrawal)

            // Then — createdBy is always the authenticated user (actor), not the target
            assertEquals(testUserId, slot.captured.createdBy)
            assertEquals("target-user", slot.captured.withdrawnBy)
        }

        @Test
        fun `sets remainingAmount to amountWithdrawn when not set`() = runTest(testDispatcher) {
            // Given
            val withdrawal = testWithdrawal.copy(remainingAmount = 0)
            val slot = slot<CashWithdrawal>()
            coEvery { localWriteDataSource.saveWithdrawal(capture(slot)) } just Runs

            // When
            repository.addWithdrawal(testGroupId, withdrawal)

            // Then
            assertEquals(testWithdrawal.amountWithdrawn, slot.captured.remainingAmount)
        }

        @Test
        fun `syncs to cloud in background`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs
            coEvery { cloudDataSource.addWithdrawal(any(), any()) } just Runs

            // When
            repository.addWithdrawal(testGroupId, testWithdrawal)
            advanceUntilIdle()

            // Then
            coVerify { cloudDataSource.addWithdrawal(testGroupId, any()) }
        }

        @Test
        fun `cloud failure does not affect local save`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs
            coEvery { cloudDataSource.addWithdrawal(any(), any()) } throws RuntimeException("No network")

            // When - Should not throw
            repository.addWithdrawal(testGroupId, testWithdrawal)
            advanceUntilIdle()

            // Then - Local save should succeed
            coVerify { localWriteDataSource.saveWithdrawal(any()) }
        }

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs

            // When
            repository.addWithdrawal(testGroupId, testWithdrawal)

            // Then
            coVerify {
                localWriteDataSource.saveWithdrawal(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs
            coEvery { cloudDataSource.addWithdrawal(any(), any()) } just Runs
            coEvery { localWriteDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.addWithdrawal(testGroupId, testWithdrawal)
            advanceUntilIdle()

            // Then
            coVerify { localWriteDataSource.updateSyncStatus(any(), SyncStatus.SYNCED) }
        }

        @Test
        fun `updates to SYNC_FAILED after cloud sync failure`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.saveWithdrawal(any()) } just Runs
            coEvery {
                cloudDataSource.addWithdrawal(any(), any())
            } throws RuntimeException("No network")
            coEvery { localWriteDataSource.updateSyncStatus(any(), any()) } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localQueryDataSource.getWithdrawalById(any())
            } returns testWithdrawal.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.addWithdrawal(testGroupId, testWithdrawal)
            advanceUntilIdle()

            // Then
            coVerify { localWriteDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED) }
        }
    }

    @Nested
    inner class GetGroupWithdrawalsFlow {

        @Test
        fun `returns local data flow`() = runTest(testDispatcher) {
            // Given
            val localWithdrawals = listOf(testWithdrawal)
            every { localQueryDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(localWithdrawals)
            every { cloudDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(localWithdrawals)
            coEvery { localWriteDataSource.replaceWithdrawalsForGroup(any(), any()) } just Runs

            // When
            val flow = repository.getGroupWithdrawalsFlow(testGroupId)
            val result = flow.first()
            advanceUntilIdle()

            // Then
            assertEquals(1, result.size)
            assertEquals(testWithdrawal.id, result[0].id)
        }
    }

    @Nested
    inner class RefundTranche {

        @Test
        fun `refunds amount back to withdrawal`() = runTest(testDispatcher) {
            // Given
            val withdrawal = testWithdrawal.copy(remainingAmount = 500000L)
            coEvery { localQueryDataSource.getWithdrawalById("w-1") } returns withdrawal
            coEvery { localWriteDataSource.updateRemainingAmount(any(), any()) } just Runs

            // When
            repository.refundTranche("w-1", 200000L)
            advanceUntilIdle()

            // Then - Should update with original remaining + refunded amount
            coVerify { localWriteDataSource.updateRemainingAmount("w-1", 700000L) }
        }
    }

    @Nested
    inner class DeleteWithdrawal {

        @Test
        fun `deletes from local first`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.deleteWithdrawal("w-1") } just Runs
            coEvery { cloudDataSource.deleteWithdrawal(any(), any()) } just Runs

            // When
            repository.deleteWithdrawal(testGroupId, "w-1")
            advanceUntilIdle()

            // Then
            coVerifyOrder {
                localWriteDataSource.deleteWithdrawal("w-1")
                cloudDataSource.deleteWithdrawal(testGroupId, "w-1")
            }
        }

        @Test
        fun `always queues cloud deletion regardless of sync status`() = runTest(testDispatcher) {
            // Given — deleteWithdrawal() no longer checks sync status; it always queues
            // the cloud deletion so Firestore SDK handles write ordering.
            val withdrawalId = "any-w"
            coEvery { localWriteDataSource.deleteWithdrawal(withdrawalId) } just Runs
            coEvery { cloudDataSource.deleteWithdrawal(any(), any()) } just Runs

            // When
            repository.deleteWithdrawal(testGroupId, withdrawalId)
            advanceUntilIdle()

            // Then — cloud deletion is always queued
            coVerify(exactly = 1) {
                cloudDataSource.deleteWithdrawal(testGroupId, withdrawalId)
            }
            coVerify(exactly = 1) { localWriteDataSource.deleteWithdrawal(withdrawalId) }
        }

        @Test
        fun `cloud failure does not affect local delete`() = runTest(testDispatcher) {
            // Given
            coEvery { localWriteDataSource.deleteWithdrawal("w-1") } just Runs
            coEvery {
                cloudDataSource.deleteWithdrawal(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.deleteWithdrawal(testGroupId, "w-1")
            advanceUntilIdle()

            // Then - Local delete should still have happened
            coVerify(exactly = 1) { localWriteDataSource.deleteWithdrawal("w-1") }
        }
    }

    @Nested
    inner class ConfirmPendingSyncWithdrawals {

        @Test
        fun `transitions PENDING_SYNC withdrawals to SYNCED when server confirms`() = runTest(testDispatcher) {
            // Given — cloud returns withdrawals, local has pending sync IDs
            val localWithdrawals = listOf(testWithdrawal)
            every { localQueryDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(localWithdrawals)
            coEvery { localWriteDataSource.replaceWithdrawalsForGroup(any(), any()) } just Runs
            coEvery { localQueryDataSource.getPendingSyncWithdrawalIds(testGroupId) } returns listOf("pending-1")
            coEvery { cloudDataSource.verifyWithdrawalOnServer(testGroupId, "pending-1") } returns true
            coEvery { localWriteDataSource.updateSyncStatus(any(), any()) } just Runs

            // When — trigger the flow to start the cloud subscription
            repository.getGroupWithdrawalsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — pending withdrawal should be confirmed as SYNCED
            coVerify { localWriteDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED) }
        }

        @Test
        fun `keeps PENDING_SYNC when server verification fails`() = runTest(testDispatcher) {
            // Given
            every { localQueryDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(listOf(testWithdrawal))
            coEvery { localWriteDataSource.replaceWithdrawalsForGroup(any(), any()) } just Runs
            coEvery { localQueryDataSource.getPendingSyncWithdrawalIds(testGroupId) } returns listOf("pending-1")
            coEvery {
                cloudDataSource.verifyWithdrawalOnServer(testGroupId, "pending-1")
            } throws RuntimeException("Server unreachable")

            // When
            repository.getGroupWithdrawalsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should NOT update sync status
            coVerify(exactly = 0) {
                localWriteDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `skips when no pending withdrawals exist`() = runTest(testDispatcher) {
            // Given
            every { localQueryDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudDataSource.getWithdrawalsByGroupIdFlow(testGroupId) } returns flowOf(listOf(testWithdrawal))
            coEvery { localWriteDataSource.replaceWithdrawalsForGroup(any(), any()) } just Runs
            coEvery { localQueryDataSource.getPendingSyncWithdrawalIds(testGroupId) } returns emptyList()

            // When
            repository.getGroupWithdrawalsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should not attempt any verification
            coVerify(exactly = 0) { cloudDataSource.verifyWithdrawalOnServer(any(), any()) }
        }
    }
}
