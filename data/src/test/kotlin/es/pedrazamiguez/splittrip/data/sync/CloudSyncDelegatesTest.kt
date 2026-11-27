package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CloudSyncDelegates")
class CloudSyncDelegatesTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockPerformanceMonitor = mockk<PerformanceMonitor>(relaxed = true) {
        io.mockk.coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        io.mockk.every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
    }

    @Nested
    @DisplayName("subscribeAndReconcile")
    inner class SubscribeAndReconcileTests {

        @Test
        fun `reconciles local with remote items on each emission`() = runTest(testDispatcher) {
            val remoteItems = listOf("item-1", "item-2")
            val reconciled = mutableListOf<List<String>>()

            subscribeAndReconcile(
                cloudFlow = flowOf(remoteItems),
                params = SyncReconciliationParams(
                    reconcileLocal = { reconciled.add(it) },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "for test",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertEquals(1, reconciled.size)
            assertEquals(remoteItems, reconciled[0])
        }

        @Test
        fun `calls confirmPendingSync after reconciliation`() = runTest(testDispatcher) {
            val confirmedIds = mutableListOf<String>()

            subscribeAndReconcile(
                cloudFlow = flowOf(listOf("item-1")),
                params = SyncReconciliationParams(
                    reconcileLocal = { },
                    getPendingIds = { listOf("pending-1", "pending-2") },
                    verifyOnServer = { true },
                    markSynced = { confirmedIds.add(it) },
                    entityLabel = "test",
                    logContext = "",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertEquals(listOf("pending-1", "pending-2"), confirmedIds)
        }

        @Test
        fun `continues collecting after reconciliation error`() = runTest(testDispatcher) {
            // The delegate catches inner exceptions — the outer flow keeps collecting.
            // With flowOf, there's only one emission, so we just verify it doesn't throw.
            var reconcileCallCount = 0

            subscribeAndReconcile(
                cloudFlow = flowOf(listOf("item-1")),
                params = SyncReconciliationParams(
                    reconcileLocal = {
                        reconcileCallCount++
                        throw IOException("Reconciliation error")
                    },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertEquals(1, reconcileCallCount)
        }

        @Test
        fun `skips pending sync confirmation when no pending ids exist`() =
            runTest(testDispatcher) {
                var verifyOnServerCalled = false

                subscribeAndReconcile(
                    cloudFlow = flowOf(listOf("item-1")),
                    params = SyncReconciliationParams(
                        reconcileLocal = { },
                        getPendingIds = { emptyList() },
                        verifyOnServer = {
                            verifyOnServerCalled = true
                            true
                        },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )

                assertEquals(false, verifyOnServerCalled)
            }

        @Test
        fun `handles exception thrown by cloud flow itself`() = runTest(testDispatcher) {
            var reconcileCallCount = 0

            subscribeAndReconcile(
                cloudFlow = flow<List<String>> { throw IOException("Cloud flow error") },
                params = SyncReconciliationParams(
                    reconcileLocal = { reconcileCallCount++ },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "test context",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            // Should complete without throwing; reconcileLocal never reached
            assertEquals(0, reconcileCallCount)
        }

        @Test
        fun `rethrows CancellationException from cloud flow`() = runTest(testDispatcher) {
            val thrown = runCatching {
                subscribeAndReconcile(
                    cloudFlow = flow<List<String>> { throw CancellationException("Flow cancelled") },
                    params = SyncReconciliationParams(
                        reconcileLocal = { },
                        getPendingIds = { emptyList() },
                        verifyOnServer = { true },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )
            }.exceptionOrNull()

            assertInstanceOf(CancellationException::class.java, thrown)
        }
    }

    @Nested
    @DisplayName("confirmPendingSync")
    inner class ConfirmPendingSyncTests {

        @Test
        fun `marks verified items as synced`() = runTest(testDispatcher) {
            val syncedIds = mutableListOf<String>()

            confirmPendingSync(
                getPendingIds = { listOf("id-1", "id-2") },
                verifyOnServer = { true },
                markSynced = { syncedIds.add(it) },
                entityLabel = "test"
            )

            assertEquals(listOf("id-1", "id-2"), syncedIds)
        }

        @Test
        fun `does not mark items when server verification returns false`() =
            runTest(testDispatcher) {
                val syncedIds = mutableListOf<String>()

                confirmPendingSync(
                    getPendingIds = { listOf("id-1") },
                    verifyOnServer = { false },
                    markSynced = { syncedIds.add(it) },
                    entityLabel = "test"
                )

                assertEquals(emptyList<String>(), syncedIds)
            }

        @Test
        fun `does not mark items when server verification throws`() = runTest(testDispatcher) {
            val syncedIds = mutableListOf<String>()

            confirmPendingSync(
                getPendingIds = { listOf("id-1") },
                verifyOnServer = { throw IOException("Server unreachable") },
                markSynced = { syncedIds.add(it) },
                entityLabel = "test"
            )

            assertEquals(emptyList<String>(), syncedIds)
        }

        @Test
        fun `skips entirely when no pending ids exist`() = runTest(testDispatcher) {
            var verifyAttempted = false

            confirmPendingSync(
                getPendingIds = { emptyList() },
                verifyOnServer = {
                    verifyAttempted = true
                    true
                },
                markSynced = { },
                entityLabel = "test"
            )

            assertEquals(false, verifyAttempted)
        }

        @Test
        fun `continues to next item when one verification fails`() = runTest(testDispatcher) {
            val syncedIds = mutableListOf<String>()
            var callCount = 0

            confirmPendingSync(
                getPendingIds = { listOf("id-1", "id-2", "id-3") },
                verifyOnServer = { id ->
                    callCount++
                    if (id == "id-2") throw IOException("Error")
                    true
                },
                markSynced = { syncedIds.add(it) },
                entityLabel = "test"
            )

            assertEquals(3, callCount, "All items should be attempted")
            assertEquals(listOf("id-1", "id-3"), syncedIds)
        }

        @Test
        fun `rethrows CancellationException thrown by verifyOnServer`() = runTest(testDispatcher) {
            val thrown = runCatching {
                confirmPendingSync(
                    getPendingIds = { listOf("id-1") },
                    verifyOnServer = { throw CancellationException("Verify cancelled") },
                    markSynced = { },
                    entityLabel = "test"
                )
            }.exceptionOrNull()

            assertInstanceOf(CancellationException::class.java, thrown)
        }
    }

    @Nested
    @DisplayName("syncCreateToCloud")
    inner class SyncCreateToCloudTests {

        @Test
        fun `calls cloud write and marks SYNCED on success`() = runTest(testDispatcher) {
            val updateSyncStatus: suspend (String, SyncStatus) -> Unit = mockk(relaxed = true)
            val cloudWrite: suspend () -> Unit = mockk(relaxed = true)

            syncCreateToCloud(
                scope = this,
                entityId = "entity-1",
                params = SyncCreateParams(
                    cloudWrite = cloudWrite,
                    updateSyncStatus = updateSyncStatus,
                    entityLabel = "test",
                    performanceMonitor = mockPerformanceMonitor
                )
            )
            advanceUntilIdle()

            coVerifyOrder {
                cloudWrite()
                updateSyncStatus("entity-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `marks SYNC_FAILED when cloud write throws`() = runTest(testDispatcher) {
            val updateSyncStatus: suspend (String, SyncStatus) -> Unit = mockk(relaxed = true)

            syncCreateToCloud(
                scope = this,
                entityId = "entity-1",
                params = SyncCreateParams(
                    cloudWrite = { throw IOException("Network error") },
                    updateSyncStatus = updateSyncStatus,
                    entityLabel = "test",
                    performanceMonitor = mockPerformanceMonitor
                )
            )
            advanceUntilIdle()

            coVerify {
                updateSyncStatus("entity-1", SyncStatus.SYNC_FAILED)
            }
            coVerify(exactly = 0) {
                updateSyncStatus("entity-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `does not block the caller`() = runTest(testDispatcher) {
            var cloudWriteCalled = false

            syncCreateToCloud(
                scope = this,
                entityId = "entity-1",
                params = SyncCreateParams(
                    cloudWrite = { cloudWriteCalled = true },
                    updateSyncStatus = { _, _ -> },
                    entityLabel = "test",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            // Before advanceUntilIdle, cloud write should not have completed yet
            assertEquals(false, cloudWriteCalled, "Cloud write runs in background")

            advanceUntilIdle()
            assertEquals(true, cloudWriteCalled)
        }

        @Test
        fun `does not mark SYNC_FAILED when entity is already SYNCED`() = runTest(testDispatcher) {
            val updateSyncStatus: suspend (String, SyncStatus) -> Unit = mockk(relaxed = true)

            syncCreateToCloud(
                scope = this,
                entityId = "entity-1",
                params = SyncCreateParams(
                    cloudWrite = { throw IOException("Network error") },
                    updateSyncStatus = updateSyncStatus,
                    getCurrentSyncStatus = { SyncStatus.SYNCED },
                    entityLabel = "test",
                    performanceMonitor = mockPerformanceMonitor
                )
            )
            advanceUntilIdle()

            // Entity is already SYNCED — must not be downgraded to SYNC_FAILED
            coVerify(exactly = 0) { updateSyncStatus(any(), any()) }
        }

        @Test
        fun `does not mark SYNC_FAILED when cloudWrite throws CancellationException`() =
            runTest(testDispatcher) {
                val updateSyncStatus: suspend (String, SyncStatus) -> Unit = mockk(relaxed = true)

                syncCreateToCloud(
                    scope = this,
                    entityId = "entity-1",
                    params = SyncCreateParams(
                        cloudWrite = { throw CancellationException("Job cancelled") },
                        updateSyncStatus = updateSyncStatus,
                        entityLabel = "test",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )
                advanceUntilIdle()

                // CancellationException is rethrown, so neither SYNCED nor SYNC_FAILED is called
                coVerify(exactly = 0) { updateSyncStatus(any(), any()) }
            }
    }

    @Nested
    @DisplayName("syncDeletionToCloud")
    inner class SyncDeletionToCloudTests {

        @Test
        fun `calls cloud delete on success`() = runTest(testDispatcher) {
            val cloudDelete: suspend () -> Unit = mockk(relaxed = true)

            syncDeletionToCloud(
                scope = this,
                entityId = "entity-1",
                cloudDelete = cloudDelete,
                entityLabel = "test"
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { cloudDelete() }
        }

        @Test
        fun `does not throw when cloud delete fails`() = runTest(testDispatcher) {
            // Should not propagate the exception
            syncDeletionToCloud(
                scope = this,
                entityId = "entity-1",
                cloudDelete = { throw IOException("Network error") },
                entityLabel = "test"
            )
            advanceUntilIdle()

            // Test passes if no exception is thrown
        }

        @Test
        fun `does not block the caller`() = runTest(testDispatcher) {
            var cloudDeleteCalled = false

            syncDeletionToCloud(
                scope = this,
                entityId = "entity-1",
                cloudDelete = { cloudDeleteCalled = true },
                entityLabel = "test"
            )

            assertEquals(false, cloudDeleteCalled, "Cloud delete runs in background")

            advanceUntilIdle()
            assertEquals(true, cloudDeleteCalled)
        }

        @Test
        fun `launched job is cancelled when cloudDelete throws CancellationException`() =
            runTest(testDispatcher) {
                // Capture existing children so we can isolate the one launched by syncDeletionToCloud.
                // With StandardTestDispatcher the coroutine is queued but not yet running after the
                // call returns, so the child Job is already registered with the scope.
                val existingChildren = this.coroutineContext[Job]!!.children.toList()

                syncDeletionToCloud(
                    scope = this,
                    entityId = "entity-1",
                    cloudDelete = { throw CancellationException("Delete cancelled") },
                    entityLabel = "test"
                )

                val launchedJob = this.coroutineContext[Job]!!.children
                    .first { it !in existingChildren }

                advanceUntilIdle()

                // If CancellationException is rethrown (not swallowed by catch(Exception)),
                // the coroutine is cancelled → isCancelled = true.
                // If it were swallowed, the coroutine would complete normally → isCancelled = false.
                assertTrue(launchedJob.isCancelled, "Coroutine should be cancelled, not completed normally")
            }
    }

    @Nested
    @DisplayName("fetchOnceAndReconcile")
    inner class FetchOnceAndReconcileTests {

        @Test
        fun `fetches once, reconciles local, and caches groupId in syncedArchivedGroupIds`() = runTest(testDispatcher) {
            val syncedIds = mutableSetOf<String>()
            val remoteItems = listOf("item-1", "item-2")
            val reconciled = mutableListOf<List<String>>()
            var fetchCalls = 0

            fetchOnceAndReconcile(
                groupId = "group-1",
                syncedArchivedGroupIds = syncedIds,
                fetchOnce = {
                    fetchCalls++
                    remoteItems
                },
                params = SyncReconciliationParams(
                    reconcileLocal = { reconciled.add(it) },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "for group-1",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertEquals(1, fetchCalls)
            assertEquals(1, reconciled.size)
            assertEquals(remoteItems, reconciled[0])
            assertTrue("group-1" in syncedIds)
        }

        @Test
        fun `skips cloud fetch when groupId is already in syncedArchivedGroupIds`() = runTest(testDispatcher) {
            val syncedIds = mutableSetOf("group-1")
            var fetchCalled = false
            var reconcileCalled = false

            fetchOnceAndReconcile(
                groupId = "group-1",
                syncedArchivedGroupIds = syncedIds,
                fetchOnce = {
                    fetchCalled = true
                    listOf("item-1")
                },
                params = SyncReconciliationParams(
                    reconcileLocal = { reconcileCalled = true },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertFalse(fetchCalled)
            assertFalse(reconcileCalled)
        }

        @Test
        fun `does not cache groupId when fetchOnce throws exception`() = runTest(testDispatcher) {
            val syncedIds = mutableSetOf<String>()

            fetchOnceAndReconcile<String>(
                groupId = "group-1",
                syncedArchivedGroupIds = syncedIds,
                fetchOnce = { throw IOException("Network error") },
                params = SyncReconciliationParams(
                    reconcileLocal = { },
                    getPendingIds = { emptyList() },
                    verifyOnServer = { true },
                    markSynced = { },
                    entityLabel = "test",
                    logContext = "",
                    performanceMonitor = mockPerformanceMonitor
                )
            )

            assertTrue("group-1" !in syncedIds)
        }

        @Test
        fun `skips reconciliation and does not cache groupId when verifyParentExists returns false`() =
            runTest(testDispatcher) {
                val syncedIds = mutableSetOf<String>()
                var reconcileCalled = false

                fetchOnceAndReconcile(
                    groupId = "group-1",
                    syncedArchivedGroupIds = syncedIds,
                    fetchOnce = { listOf("item-1") },
                    params = SyncReconciliationParams(
                        reconcileLocal = { reconcileCalled = true },
                        getPendingIds = { emptyList() },
                        verifyOnServer = { true },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor,
                        verifyParentExists = { false }
                    )
                )

                assertFalse(reconcileCalled)
                assertTrue("group-1" !in syncedIds)
            }

        @Test
        fun `rethrows CancellationException from fetchOnce`() = runTest(testDispatcher) {
            val syncedIds = mutableSetOf<String>()

            val thrown = runCatching {
                fetchOnceAndReconcile<String>(
                    groupId = "group-1",
                    syncedArchivedGroupIds = syncedIds,
                    fetchOnce = { throw CancellationException("Cancelled") },
                    params = SyncReconciliationParams(
                        reconcileLocal = { },
                        getPendingIds = { emptyList() },
                        verifyOnServer = { true },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )
            }.exceptionOrNull()

            assertInstanceOf(CancellationException::class.java, thrown)
        }
    }

    @Nested
    @DisplayName("syncGroupCollection")
    inner class SyncGroupCollectionTests {

        @Test
        fun `uses fetchOnceAndReconcile without collecting cloudFlow when group is archived`() =
            runTest(testDispatcher) {
                val syncedIds = mutableSetOf<String>()
                var fetchCalls = 0
                var cloudFlowCollected = false

                syncGroupCollection(
                    groupId = "group-1",
                    isGroupArchived = { true },
                    syncedArchivedGroupIds = syncedIds,
                    fetchOnce = {
                        fetchCalls++
                        listOf("item-1")
                    },
                    cloudFlow = {
                        flow {
                            cloudFlowCollected = true
                            emit(listOf("item-1"))
                        }
                    },
                    params = SyncReconciliationParams(
                        reconcileLocal = { },
                        getPendingIds = { emptyList() },
                        verifyOnServer = { true },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )

                assertEquals(1, fetchCalls)
                assertFalse(cloudFlowCollected)
                assertTrue("group-1" in syncedIds)
            }

        @Test
        fun `uses subscribeAndReconcile without calling fetchOnce when group is active`() =
            runTest(testDispatcher) {
                val syncedIds = mutableSetOf<String>()
                var fetchCalled = false
                var cloudFlowCollected = false

                syncGroupCollection(
                    groupId = "group-1",
                    isGroupArchived = { false },
                    syncedArchivedGroupIds = syncedIds,
                    fetchOnce = {
                        fetchCalled = true
                        listOf("item-1")
                    },
                    cloudFlow = {
                        flow {
                            cloudFlowCollected = true
                            emit(listOf("item-1"))
                        }
                    },
                    params = SyncReconciliationParams(
                        reconcileLocal = { },
                        getPendingIds = { emptyList() },
                        verifyOnServer = { true },
                        markSynced = { },
                        entityLabel = "test",
                        logContext = "",
                        performanceMonitor = mockPerformanceMonitor
                    )
                )

                assertTrue(cloudFlowCollected)
                assertFalse(fetchCalled)
                assertTrue("group-1" !in syncedIds)
            }
    }
}
