package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepositoryImplTest {

    private lateinit var cloudExpenseDataSource: CloudExpenseDataSource
    private lateinit var localExpenseDataSource: LocalExpenseDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var cloudStorageDataSource:
        es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: ExpenseRepositoryImpl

    private val testUserId = "user-123"
    private val testGroupId = "group-123"
    private val testExpense = Expense(
        id = "expense-1",
        groupId = testGroupId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        createdBy = "user-1",
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 30)
    )

    private val cloudExpenses = listOf(
        testExpense,
        Expense(
            id = "expense-2",
            groupId = testGroupId,
            title = "Taxi",
            sourceAmount = 2000L,
            sourceCurrency = "EUR",
            groupAmount = 2000L,
            groupCurrency = "EUR",
            paymentMethod = PaymentMethod.CASH,
            createdBy = "user-2",
            createdAt = LocalDateTime.of(2024, 1, 16, 10, 0)
        )
    )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        cloudExpenseDataSource = mockk()
        localExpenseDataSource = mockk(relaxed = true)
        authenticationService = mockk()
        cloudStorageDataSource = mockk(relaxed = true)
        every { authenticationService.currentUserId() } returns testUserId
        repository = ExpenseRepositoryImpl(
            cloudExpenseDataSource,
            localExpenseDataSource,
            authenticationService,
            cloudStorageDataSource,
            testDispatcher
        )
    }

    @Nested
    inner class AddExpense {

        @Test
        fun `addExpense saves to local first`() = runTest(testDispatcher) {
            // Given
            val expenseWithoutGroup = testExpense.copy(groupId = "")
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithoutGroup)
            advanceTimeBy(100) // Allow local save to complete

            // Then - Local save should happen immediately
            coVerify { localExpenseDataSource.saveExpense(any()) }
            assertEquals(testGroupId, expenseSlot.captured.groupId)
        }

        @Test
        fun `addExpense generates UUID when expense has blank ID`() = runTest(testDispatcher) {
            // Given
            val expenseWithBlankId = testExpense.copy(id = "")
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithBlankId)
            advanceTimeBy(100)

            // Then - Should generate a valid UUID
            assertNotNull(expenseSlot.captured.id)
            assertTrue(expenseSlot.captured.id.isNotBlank())
        }

        @Test
        fun `addExpense preserves existing ID when present`() = runTest(testDispatcher) {
            // Given
            val existingId = "existing-id-123"
            val expenseWithId = testExpense.copy(id = existingId)
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithId)
            advanceTimeBy(100)

            // Then - Should keep the existing ID
            assertEquals(existingId, expenseSlot.captured.id)
        }

        @Test
        fun `addExpense syncs to cloud in background`() = runTest(testDispatcher) {
            // Given
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, testExpense)
            advanceUntilIdle() // Allow background sync to complete

            // Then - Cloud sync should happen
            coVerify { cloudExpenseDataSource.addExpense(testGroupId, any()) }
        }

        @Test
        fun `addExpense local save succeeds even if cloud sync fails`() = runTest(testDispatcher) {
            // Given
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs
            coEvery {
                cloudExpenseDataSource.addExpense(
                    any(),
                    any()
                )
            } throws RuntimeException("Network error")

            // When - Should not throw exception
            repository.addExpense(testGroupId, testExpense)
            advanceUntilIdle()

            // Then - Local save should still succeed
            coVerify { localExpenseDataSource.saveExpense(any()) }
        }

        @Test
        fun `addExpense populates groupId from parameter`() = runTest(testDispatcher) {
            // Given
            val expenseWithoutGroup = testExpense.copy(groupId = "")
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithoutGroup)
            advanceTimeBy(100)

            // Then
            assertEquals(testGroupId, expenseSlot.captured.groupId)
        }

        @Test
        fun `addExpense populates createdBy with current user ID when blank`() = runTest(testDispatcher) {
            // Given
            val expenseWithoutCreatedBy = testExpense.copy(createdBy = "")
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithoutCreatedBy)
            advanceTimeBy(100)

            // Then - Should populate with authenticated user ID
            assertEquals(testUserId, expenseSlot.captured.createdBy)
        }

        @Test
        fun `addExpense preserves existing createdBy when present`() = runTest(testDispatcher) {
            // Given
            val existingUserId = "existing-user-456"
            val expenseWithCreatedBy = testExpense.copy(createdBy = existingUserId)
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithCreatedBy)
            advanceTimeBy(100)

            // Then - Should keep existing createdBy
            assertEquals(existingUserId, expenseSlot.captured.createdBy)
        }

        @Test
        fun `addExpense generates timestamps when missing`() = runTest(testDispatcher) {
            // Given
            val expenseWithoutTimestamps = testExpense.copy(createdAt = null, lastUpdatedAt = null)
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, expenseWithoutTimestamps)
            advanceTimeBy(100)

            // Then - Should have timestamps
            assertNotNull(expenseSlot.captured.createdAt)
            assertNotNull(expenseSlot.captured.lastUpdatedAt)
        }

        @Test
        fun `addExpense works offline with all metadata populated`() = runTest(testDispatcher) {
            // Given - Offline scenario: expense with no ID, no createdBy, no timestamps
            val offlineExpense = Expense(
                id = "",
                groupId = "",
                title = "Offline Expense",
                sourceAmount = 3000L,
                sourceCurrency = "EUR",
                groupAmount = 3000L,
                groupCurrency = "EUR",
                createdBy = "",
                createdAt = null,
                lastUpdatedAt = null
            )
            val expenseSlot = slot<Expense>()
            coEvery { localExpenseDataSource.saveExpense(capture(expenseSlot)) } just Runs
            coEvery {
                cloudExpenseDataSource.addExpense(
                    any(),
                    any()
                )
            } throws RuntimeException("No network")

            // When
            repository.addExpense(testGroupId, offlineExpense)
            advanceUntilIdle()

            // Then - All metadata should be populated for offline use
            val saved = expenseSlot.captured
            assertTrue(saved.id.isNotBlank(), "ID should be generated")
            assertEquals(testGroupId, saved.groupId)
            assertEquals(testUserId, saved.createdBy)
            assertNotNull(saved.createdAt)
            assertNotNull(saved.lastUpdatedAt)

            // Verify local save succeeded despite cloud failure
            coVerify { localExpenseDataSource.saveExpense(any()) }
        }

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs

            // When
            repository.addExpense(testGroupId, testExpense)

            // Then
            coVerify {
                localExpenseDataSource.saveExpense(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync`() = runTest(testDispatcher) {
            // Given
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs
            coEvery { cloudExpenseDataSource.addExpense(any(), any()) } just Runs
            coEvery { localExpenseDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.addExpense(testGroupId, testExpense)
            advanceUntilIdle()

            // Then
            coVerify {
                localExpenseDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
        }

        @Test
        fun `updates to SYNC_FAILED after cloud sync failure`() = runTest(testDispatcher) {
            // Given
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs
            coEvery {
                cloudExpenseDataSource.addExpense(any(), any())
            } throws RuntimeException("Network error")
            coEvery { localExpenseDataSource.updateSyncStatus(any(), any()) } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localExpenseDataSource.getExpenseById(any())
            } returns testExpense.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.addExpense(testGroupId, testExpense)
            advanceUntilIdle()

            // Then
            coVerify {
                localExpenseDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED)
            }
        }
    }

    @Nested
    inner class GetGroupExpensesFlow {

        @Test
        fun `returns local data immediately`() = runTest(testDispatcher) {
            // Given - Local data is available, cloud listener exists
            val localExpenses = listOf(testExpense)
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                localExpenses
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                cloudExpenses
            )
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When
            val flow = repository.getGroupExpensesFlow(testGroupId)
            val result = flow.first()

            // Then - Should return local data immediately
            assertEquals(1, result.size)
            assertEquals(testExpense.id, result[0].id)
        }

        @Test
        fun `subscribes to real-time cloud changes on flow start`() = runTest(testDispatcher) {
            // Given
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                emptyList()
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                cloudExpenses
            )
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When
            val flow = repository.getGroupExpensesFlow(testGroupId)
            flow.first() // Trigger flow collection
            advanceUntilIdle() // Allow background subscription to process

            // Then - Cloud real-time listener should be used (not one-shot fetch)
            coVerify { localExpenseDataSource.replaceExpensesForGroup(testGroupId, cloudExpenses) }
        }

        @Test
        fun `cloud sync reconciles local with full replace`() = runTest(testDispatcher) {
            // Given
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                emptyList()
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                cloudExpenses
            )
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When
            val flow = repository.getGroupExpensesFlow(testGroupId)
            flow.first()
            advanceUntilIdle()

            // Then - Should use replaceExpensesForGroup (not upsert) to handle deletions
            coVerify { localExpenseDataSource.replaceExpensesForGroup(testGroupId, cloudExpenses) }
        }

        @Test
        fun `cloud sync failure does not affect local data flow`() = runTest(testDispatcher) {
            // Given
            val localExpenses = listOf(testExpense)
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                localExpenses
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flow {
                throw IOException("Network error")
            }

            // When - Should not throw exception
            val flow = repository.getGroupExpensesFlow(testGroupId)
            val result = flow.first()
            advanceUntilIdle()

            // Then - Should still return local data
            assertNotNull(result)
            assertEquals(1, result.size)
            assertEquals(testExpense.id, result[0].id)
        }
    }

    @Nested
    inner class OfflineFirstBehavior {

        @Test
        fun `offline mode uses only local data`() = runTest(testDispatcher) {
            // Given - Local has data, cloud is unavailable
            val localExpenses = listOf(testExpense)
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                localExpenses
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flow {
                throw IOException("No network")
            }

            // When
            val flow = repository.getGroupExpensesFlow(testGroupId)
            val result = flow.first()
            advanceUntilIdle()

            // Then - Should return local data successfully
            assertEquals(1, result.size)
            assertEquals(testExpense.id, result[0].id)
        }

        @Test
        fun `local writes succeed immediately regardless of cloud status`() = runTest(testDispatcher) {
            // Given - Cloud is unavailable
            coEvery { localExpenseDataSource.saveExpense(any()) } just Runs
            coEvery {
                cloudExpenseDataSource.addExpense(
                    any(),
                    any()
                )
            } throws RuntimeException("No network")

            // When - Should not throw
            repository.addExpense(testGroupId, testExpense)
            advanceTimeBy(100)

            // Then - Local save should succeed
            coVerify { localExpenseDataSource.saveExpense(any()) }
        }

        @Test
        fun `stale local data is returned before cloud sync completes`() = runTest(testDispatcher) {
            // Given - Local has old data, cloud has new data but is slow
            val staleExpense = testExpense.copy(title = "Old Title")
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                listOf(staleExpense)
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flow {
                delay(1000) // Simulate slow network
                emit(cloudExpenses)
            }
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When - Get data immediately
            val flow = repository.getGroupExpensesFlow(testGroupId)
            val immediateResult = flow.first()

            // Then - Should get stale local data immediately without waiting for cloud
            assertEquals("Old Title", immediateResult[0].title)
        }
    }

    @Nested
    inner class SingleSourceOfTruth {

        @Test
        fun `local database is the single source of truth`() = runTest(testDispatcher) {
            // Given - Different data in local and cloud
            val localExpenses = listOf(testExpense.copy(title = "Local Title"))
            val cloudExpensesList = listOf(testExpense.copy(title = "Cloud Title"))

            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                localExpenses
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                cloudExpensesList
            )
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When - Get data
            val flow = repository.getGroupExpensesFlow(testGroupId)
            val result = flow.first()

            // Then - Should return local data (SSOT)
            assertEquals("Local Title", result[0].title)
        }

        @Test
        fun `cloud data replaces local to handle deletions and additions`() = runTest(testDispatcher) {
            // Given - Cloud has different set of expenses
            val initialLocal = listOf(testExpense.copy(title = "Initial"))
            val cloudUpdate = listOf(testExpense.copy(title = "Updated"))

            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                initialLocal
            )
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(
                cloudUpdate
            )
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs

            // When
            val flow = repository.getGroupExpensesFlow(testGroupId)
            flow.first()
            advanceUntilIdle()

            // Then - Cloud data should replace local (not just upsert)
            coVerify { localExpenseDataSource.replaceExpensesForGroup(testGroupId, cloudUpdate) }
        }
    }

    @Nested
    inner class DeleteExpense {

        @Test
        fun `deletes from local storage first`() = runTest(testDispatcher) {
            // Given
            val expenseId = "expense-1"
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(any(), any()) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)

            // Then - Local delete should happen immediately
            coVerify(exactly = 1) { localExpenseDataSource.deleteExpense(expenseId) }
        }

        @Test
        fun `syncs deletion to cloud in background`() = runTest(testDispatcher) {
            // Given
            val expenseId = "expense-1"
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(testGroupId, expenseId) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then - Cloud sync should happen in background
            coVerify(exactly = 1) { cloudExpenseDataSource.deleteExpense(testGroupId, expenseId) }
        }

        @Test
        fun `local delete completes even if cloud sync fails`() = runTest(testDispatcher) {
            // Given
            val expenseId = "expense-1"
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery {
                cloudExpenseDataSource.deleteExpense(any(), any())
            } throws RuntimeException("Network error")

            // When - Should not throw
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then - Local delete should have completed
            coVerify(exactly = 1) { localExpenseDataSource.deleteExpense(expenseId) }
        }

        @Test
        fun `deletes from local before cloud sync`() = runTest(testDispatcher) {
            // Given
            val expenseId = "expense-1"
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(testGroupId, expenseId) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then - Local should be called before cloud
            coVerifyOrder {
                localExpenseDataSource.deleteExpense(expenseId)
                cloudExpenseDataSource.deleteExpense(testGroupId, expenseId)
            }
        }

        @Test
        fun `passes correct groupId and expenseId to cloud`() = runTest(testDispatcher) {
            // Given
            val groupId = "specific-group"
            val expenseId = "specific-expense"
            coEvery { localExpenseDataSource.deleteExpense(any()) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(any(), any()) } just Runs

            // Re-create repository with same dispatcher for this specific test
            val repo = ExpenseRepositoryImpl(
                cloudExpenseDataSource,
                localExpenseDataSource,
                authenticationService,
                cloudStorageDataSource,
                testDispatcher
            )

            // When
            repo.deleteExpense(groupId, expenseId)
            advanceUntilIdle()

            // Then
            coVerify { cloudExpenseDataSource.deleteExpense(groupId, expenseId) }
        }

        @Test
        fun `always queues cloud deletion regardless of sync status`() = runTest(testDispatcher) {
            // Given — deleteExpense() no longer checks sync status; it always queues
            // the cloud deletion so Firestore SDK handles write ordering.
            val expenseId = "any-expense"
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(any(), any()) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then — cloud deletion is always queued
            coVerify(exactly = 1) {
                cloudExpenseDataSource.deleteExpense(testGroupId, expenseId)
            }
            coVerify(exactly = 1) { localExpenseDataSource.deleteExpense(expenseId) }
        }

        @Test
        fun `syncs to cloud when expense is SYNCED`() = runTest(testDispatcher) {
            // Given
            val expenseId = "synced-expense"
            val syncedExpense = testExpense.copy(
                id = expenseId,
                syncStatus = SyncStatus.SYNCED
            )
            coEvery { localExpenseDataSource.getExpenseById(expenseId) } returns syncedExpense
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(any(), any()) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then — cloud deletion should happen
            coVerify(exactly = 1) {
                cloudExpenseDataSource.deleteExpense(testGroupId, expenseId)
            }
        }

        @Test
        fun `syncs to cloud when expense not found locally`() = runTest(testDispatcher) {
            // Given — expense not found (null syncStatus != PENDING_SYNC)
            val expenseId = "unknown-expense"
            coEvery { localExpenseDataSource.getExpenseById(expenseId) } returns null
            coEvery { localExpenseDataSource.deleteExpense(expenseId) } just Runs
            coEvery { cloudExpenseDataSource.deleteExpense(any(), any()) } just Runs

            // When
            repository.deleteExpense(testGroupId, expenseId)
            advanceUntilIdle()

            // Then — cloud deletion should still happen
            coVerify(exactly = 1) {
                cloudExpenseDataSource.deleteExpense(testGroupId, expenseId)
            }
        }
    }

    @Nested
    inner class ConfirmPendingSyncExpenses {

        @Test
        fun `transitions PENDING_SYNC expenses to SYNCED when server confirms`() = runTest(testDispatcher) {
            // Given — cloud returns expenses, local has pending sync IDs
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(cloudExpenses)
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs
            coEvery { localExpenseDataSource.getPendingSyncExpenseIds(testGroupId) } returns listOf("pending-1")
            coEvery { cloudExpenseDataSource.verifyExpenseOnServer(testGroupId, "pending-1") } returns true
            coEvery { localExpenseDataSource.updateSyncStatus(any(), any()) } just Runs

            // When — trigger the flow to start the cloud subscription
            repository.getGroupExpensesFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — pending expense should be confirmed as SYNCED
            coVerify { localExpenseDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED) }
        }

        @Test
        fun `keeps PENDING_SYNC when server verification fails`() = runTest(testDispatcher) {
            // Given
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(cloudExpenses)
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs
            coEvery { localExpenseDataSource.getPendingSyncExpenseIds(testGroupId) } returns listOf("pending-1")
            coEvery {
                cloudExpenseDataSource.verifyExpenseOnServer(testGroupId, "pending-1")
            } throws RuntimeException("Server unreachable")

            // When
            repository.getGroupExpensesFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should NOT update sync status
            coVerify(exactly = 0) {
                localExpenseDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `skips when no pending expenses exist`() = runTest(testDispatcher) {
            // Given
            every { localExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(emptyList())
            every { cloudExpenseDataSource.getExpensesByGroupIdFlow(testGroupId) } returns flowOf(cloudExpenses)
            coEvery { localExpenseDataSource.replaceExpensesForGroup(any(), any()) } just Runs
            coEvery { localExpenseDataSource.getPendingSyncExpenseIds(testGroupId) } returns emptyList()

            // When
            repository.getGroupExpensesFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should not attempt any verification
            coVerify(exactly = 0) { cloudExpenseDataSource.verifyExpenseOnServer(any(), any()) }
        }
    }
}
