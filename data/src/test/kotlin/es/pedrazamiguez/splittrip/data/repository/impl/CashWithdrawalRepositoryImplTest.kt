package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalQueryDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalWriteDataSource
import es.pedrazamiguez.splittrip.domain.enums.PayerType
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
import org.junit.jupiter.api.Assertions.assertNull
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

    @Nested
    inner class GetAvailableWithdrawals {

        @Test
        fun `GROUP scope returns group-scoped withdrawals only`() = runTest(testDispatcher) {
            val expected = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns expected

            val result = repository.getAvailableWithdrawals(testGroupId, "THB", PayerType.GROUP, null)

            assertEquals(expected, result)
            coVerify(exactly = 1) {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            }
        }

        @Test
        fun `USER scope with payerId returns user pool plus group fallback`() = runTest(testDispatcher) {
            val userPool = listOf(testWithdrawal.copy(id = "u-1"))
            val groupPool = listOf(testWithdrawal.copy(id = "g-1"))
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsUserScoped(testGroupId, "THB", testUserId)
            } returns userPool
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns groupPool

            val result = repository.getAvailableWithdrawals(
                testGroupId,
                "THB",
                PayerType.USER,
                testUserId
            )

            assertEquals(userPool + groupPool, result)
        }

        @Test
        fun `USER scope with null payerId returns empty user pool plus group fallback`() = runTest(testDispatcher) {
            val groupPool = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns groupPool

            val result = repository.getAvailableWithdrawals(testGroupId, "THB", PayerType.USER, null)

            assertEquals(groupPool, result)
            coVerify(exactly = 0) {
                localQueryDataSource.getAvailableWithdrawalsUserScoped(any(), any(), any())
            }
        }

        @Test
        fun `SUBUNIT scope with payerId returns subunit pool plus group fallback`() = runTest(testDispatcher) {
            val subunitPool = listOf(testWithdrawal.copy(id = "s-1"))
            val groupPool = listOf(testWithdrawal.copy(id = "g-1"))
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsSubunitScoped(testGroupId, "THB", "subunit-1")
            } returns subunitPool
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns groupPool

            val result = repository.getAvailableWithdrawals(
                testGroupId,
                "THB",
                PayerType.SUBUNIT,
                "subunit-1"
            )

            assertEquals(subunitPool + groupPool, result)
        }

        @Test
        fun `SUBUNIT scope with null payerId returns empty subunit pool plus group fallback`() = runTest(
            testDispatcher
        ) {
            val groupPool = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns groupPool

            val result = repository.getAvailableWithdrawals(testGroupId, "THB", PayerType.SUBUNIT, null)

            assertEquals(groupPool, result)
            coVerify(exactly = 0) {
                localQueryDataSource.getAvailableWithdrawalsSubunitScoped(any(), any(), any())
            }
        }
    }

    @Nested
    inner class GetAvailableWithdrawalsByExactScope {

        @Test
        fun `GROUP scope returns group-scoped withdrawals`() = runTest(testDispatcher) {
            val expected = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsGroupScoped(testGroupId, "THB")
            } returns expected

            val result = repository.getAvailableWithdrawalsByExactScope(
                testGroupId,
                "THB",
                PayerType.GROUP,
                null
            )

            assertEquals(expected, result)
        }

        @Test
        fun `USER scope with scopeOwnerId returns user-scoped withdrawals`() = runTest(testDispatcher) {
            val expected = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsUserScoped(testGroupId, "THB", testUserId)
            } returns expected

            val result = repository.getAvailableWithdrawalsByExactScope(
                testGroupId,
                "THB",
                PayerType.USER,
                testUserId
            )

            assertEquals(expected, result)
        }

        @Test
        fun `USER scope with null scopeOwnerId returns empty list`() = runTest(testDispatcher) {
            val result = repository.getAvailableWithdrawalsByExactScope(
                testGroupId,
                "THB",
                PayerType.USER,
                null
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `SUBUNIT scope with scopeOwnerId returns subunit-scoped withdrawals`() = runTest(testDispatcher) {
            val expected = listOf(testWithdrawal)
            coEvery {
                localQueryDataSource.getAvailableWithdrawalsSubunitScoped(testGroupId, "THB", "subunit-1")
            } returns expected

            val result = repository.getAvailableWithdrawalsByExactScope(
                testGroupId,
                "THB",
                PayerType.SUBUNIT,
                "subunit-1"
            )

            assertEquals(expected, result)
        }

        @Test
        fun `SUBUNIT scope with null scopeOwnerId returns empty list`() = runTest(testDispatcher) {
            val result = repository.getAvailableWithdrawalsByExactScope(
                testGroupId,
                "THB",
                PayerType.SUBUNIT,
                null
            )

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class UpdateRemainingAmount {

        @Test
        fun `updates locally and syncs to cloud in background`() = runTest(testDispatcher) {
            val withdrawal = testWithdrawal.copy(remainingAmount = 800000L)
            coEvery { localWriteDataSource.updateRemainingAmount("w-1", 800000L) } just Runs
            coEvery { localQueryDataSource.getWithdrawalById("w-1") } returns withdrawal
            coEvery { cloudDataSource.updateWithdrawal(testGroupId, withdrawal) } just Runs

            repository.updateRemainingAmount("w-1", 800000L)
            advanceUntilIdle()

            coVerify { localWriteDataSource.updateRemainingAmount("w-1", 800000L) }
            coVerify { cloudDataSource.updateWithdrawal(testGroupId, withdrawal) }
        }

        @Test
        fun `cloud sync failure does not affect local update`() = runTest(testDispatcher) {
            val withdrawal = testWithdrawal.copy(remainingAmount = 500000L)
            coEvery { localWriteDataSource.updateRemainingAmount("w-1", 500000L) } just Runs
            coEvery { localQueryDataSource.getWithdrawalById("w-1") } returns withdrawal
            coEvery {
                cloudDataSource.updateWithdrawal(any(), any())
            } throws RuntimeException("No network")

            repository.updateRemainingAmount("w-1", 500000L)
            advanceUntilIdle()

            coVerify(exactly = 1) { localWriteDataSource.updateRemainingAmount("w-1", 500000L) }
        }

        @Test
        fun `skips cloud sync when withdrawal not found locally`() = runTest(testDispatcher) {
            coEvery { localWriteDataSource.updateRemainingAmount("missing", 100L) } just Runs
            coEvery { localQueryDataSource.getWithdrawalById("missing") } returns null

            repository.updateRemainingAmount("missing", 100L)
            advanceUntilIdle()

            coVerify(exactly = 0) { cloudDataSource.updateWithdrawal(any(), any()) }
        }
    }

    @Nested
    inner class UpdateRemainingAmounts {

        @Test
        fun `batches local updates and syncs all to cloud`() = runTest(testDispatcher) {
            val w1 = testWithdrawal.copy(id = "w-1", remainingAmount = 600000L)
            val w2 = testWithdrawal.copy(id = "w-2", remainingAmount = 400000L)
            coEvery { localWriteDataSource.updateRemainingAmounts(any()) } just Runs
            coEvery { cloudDataSource.updateWithdrawal(testGroupId, w1) } just Runs
            coEvery { cloudDataSource.updateWithdrawal(testGroupId, w2) } just Runs

            repository.updateRemainingAmounts(testGroupId, listOf(w1, w2))
            advanceUntilIdle()

            coVerify {
                localWriteDataSource.updateRemainingAmounts(
                    listOf("w-1" to 600000L, "w-2" to 400000L)
                )
            }
            coVerify { cloudDataSource.updateWithdrawal(testGroupId, w1) }
            coVerify { cloudDataSource.updateWithdrawal(testGroupId, w2) }
        }

        @Test
        fun `cloud failure for one withdrawal does not stop sync for others`() = runTest(testDispatcher) {
            val w1 = testWithdrawal.copy(id = "w-1", remainingAmount = 600000L)
            val w2 = testWithdrawal.copy(id = "w-2", remainingAmount = 400000L)
            coEvery { localWriteDataSource.updateRemainingAmounts(any()) } just Runs
            coEvery { cloudDataSource.updateWithdrawal(testGroupId, w1) } throws RuntimeException("Timeout")
            coEvery { cloudDataSource.updateWithdrawal(testGroupId, w2) } just Runs

            repository.updateRemainingAmounts(testGroupId, listOf(w1, w2))
            advanceUntilIdle()

            coVerify { cloudDataSource.updateWithdrawal(testGroupId, w1) }
            coVerify { cloudDataSource.updateWithdrawal(testGroupId, w2) }
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

        @Test
        fun `skips update when withdrawal not found locally`() = runTest(testDispatcher) {
            coEvery { localQueryDataSource.getWithdrawalById("missing") } returns null

            repository.refundTranche("missing", 100000L)
            advanceUntilIdle()

            coVerify(exactly = 0) { localWriteDataSource.updateRemainingAmount(any(), any()) }
        }
    }

    @Nested
    inner class GetWithdrawalById {

        @Test
        fun `returns withdrawal from local data source`() = runTest(testDispatcher) {
            coEvery { localQueryDataSource.getWithdrawalById("w-1") } returns testWithdrawal

            val result = repository.getWithdrawalById("w-1")

            assertEquals(testWithdrawal, result)
        }

        @Test
        fun `returns null when not found`() = runTest(testDispatcher) {
            coEvery { localQueryDataSource.getWithdrawalById("unknown") } returns null

            val result = repository.getWithdrawalById("unknown")

            assertNull(result)
        }
    }
}
