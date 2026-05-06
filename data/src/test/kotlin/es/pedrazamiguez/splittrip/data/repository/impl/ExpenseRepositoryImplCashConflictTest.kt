package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for [ExpenseRepositoryImpl.addCashExpense] — the optimistic-locking
 * cash expense write path introduced in Phase 2.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepositoryImplCashConflictTest {

    private lateinit var cloudExpenseDataSource: CloudExpenseDataSource
    private lateinit var localExpenseDataSource: LocalExpenseDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var repository: ExpenseRepositoryImpl

    private val testUserId = "user-123"
    private val testGroupId = "group-123"
    private val testExpense = Expense(
        id = "expense-1",
        groupId = testGroupId,
        title = "Street food",
        sourceAmount = 10000L,
        sourceCurrency = "THB",
        groupAmount = 270L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CASH,
        createdBy = testUserId,
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 30)
    )

    private val expectedRemainingAmounts: Map<String, Long> = mapOf(
        "w-1" to 500000L,
        "w-2" to 18000L
    )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        cloudExpenseDataSource = mockk()
        localExpenseDataSource = mockk(relaxed = true)
        authenticationService = mockk()
        every { authenticationService.currentUserId() } returns testUserId
        repository = ExpenseRepositoryImpl(
            cloudExpenseDataSource,
            localExpenseDataSource,
            authenticationService,
            testDispatcher
        )
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Nested
    inner class HappyPath {

        @Test
        fun `saves expense to Room before calling Firestore transaction`() = runTest(testDispatcher) {
            coEvery { cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any()) } just Runs

            repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            coVerifyOrder {
                localExpenseDataSource.saveExpense(any())
                cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
            }
        }

        @Test
        fun `marks expense as SYNCED after successful Firestore transaction`() = runTest(testDispatcher) {
            coEvery { cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any()) } just Runs

            repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            coVerify { localExpenseDataSource.updateSyncStatus(testExpense.id, SyncStatus.SYNCED) }
        }

        @Test
        fun `returns true when Firestore transaction commits successfully`() = runTest(testDispatcher) {
            coEvery { cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any()) } just Runs

            val result = repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            assertTrue(result)
        }

        @Test
        fun `passes expectedRemainingAmounts to cloud data source`() = runTest(testDispatcher) {
            val capturedAmounts = mutableListOf<Map<String, Long>>()
            coEvery {
                cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
            } coAnswers {
                capturedAmounts.add(thirdArg())
            }

            repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            assertEquals(1, capturedAmounts.size)
            assertEquals(expectedRemainingAmounts, capturedAmounts[0])
        }

        @Test
        fun `passes groupId to cloud data source`() = runTest(testDispatcher) {
            val capturedGroupIds = mutableListOf<String>()
            coEvery {
                cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
            } coAnswers {
                capturedGroupIds.add(firstArg())
            }

            repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            assertEquals(1, capturedGroupIds.size)
            assertEquals(testGroupId, capturedGroupIds[0])
        }
    }

    // ── CashConflictException path ─────────────────────────────────────────

    @Nested
    inner class ConflictDetected {

        @Test
        fun `re-throws CashConflictException when Firestore detects concurrent modification`() =
            runTest(testDispatcher) {
                coEvery {
                    cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
                } throws CashConflictException()

                var thrownException: Throwable? = null
                try {
                    repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                    advanceUntilIdle()
                } catch (e: CashConflictException) {
                    thrownException = e
                }

                assertTrue(thrownException is CashConflictException)
            }

        @Test
        fun `rolls back Room write on CashConflictException`() = runTest(testDispatcher) {
            coEvery {
                cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
            } throws CashConflictException()

            runCatching {
                repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                advanceUntilIdle()
            }

            coVerify { localExpenseDataSource.deleteExpense(testExpense.id) }
        }

        @Test
        fun `does not mark as SYNCED or SYNC_FAILED on CashConflictException`() =
            runTest(testDispatcher) {
                coEvery {
                    cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
                } throws CashConflictException()

                runCatching {
                    repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                    advanceUntilIdle()
                }

                coVerify(exactly = 0) { localExpenseDataSource.updateSyncStatus(any(), any()) }
            }
    }

    // ── Offline / network error path ──────────────────────────────────────

    @Nested
    inner class OfflineFallback {

        @Test
        fun `swallows non-conflict exceptions to preserve offline-first behavior`() =
            runTest(testDispatcher) {
                coEvery {
                    cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
                } throws RuntimeException("Network error")

                // Must NOT throw — offline-first means the user proceeds
                val result = repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                advanceUntilIdle()

                coVerify { localExpenseDataSource.saveExpense(any()) }
                assertFalse(result) // Signals caller that Firestore transaction did not commit
            }

        @Test
        fun `marks expense as SYNC_FAILED when network error occurs and status is PENDING_SYNC`() =
            runTest(testDispatcher) {
                coEvery {
                    cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
                } throws RuntimeException("Network error")
                coEvery {
                    localExpenseDataSource.getExpenseById(testExpense.id)
                } returns testExpense.copy(syncStatus = SyncStatus.PENDING_SYNC)

                repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                advanceUntilIdle()

                coVerify { localExpenseDataSource.updateSyncStatus(testExpense.id, SyncStatus.SYNC_FAILED) }
            }

        @Test
        fun `does not downgrade to SYNC_FAILED when snapshot listener already marked SYNCED`() =
            runTest(testDispatcher) {
                coEvery {
                    cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
                } throws RuntimeException("Network error")
                // Simulate: snapshotListener confirmed sync before the error was processed
                coEvery {
                    localExpenseDataSource.getExpenseById(testExpense.id)
                } returns testExpense.copy(syncStatus = SyncStatus.SYNCED)

                repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
                advanceUntilIdle()

                coVerify(exactly = 0) { localExpenseDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED) }
            }

        @Test
        fun `does not delete Room entry on network error`() = runTest(testDispatcher) {
            coEvery {
                cloudExpenseDataSource.addExpenseWithCashPreconditions(any(), any(), any())
            } throws RuntimeException("Network error")

            repository.addCashExpense(testGroupId, testExpense, expectedRemainingAmounts)
            advanceUntilIdle()

            coVerify(exactly = 0) { localExpenseDataSource.deleteExpense(any()) }
        }
    }
}
