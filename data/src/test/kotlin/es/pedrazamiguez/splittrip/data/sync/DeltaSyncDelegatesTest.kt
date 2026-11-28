package es.pedrazamiguez.splittrip.data.sync

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeltaSyncDelegatesTest {

    private lateinit var checkpointDataSource: LocalSyncCheckpointDataSource
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var archivedSet: MutableSet<String>

    private val groupId = "group-1"
    private val collectionName = SyncCollectionName.EXPENSES
    private val nowMillis = 1_750_000_000_000L

    @BeforeEach
    fun setUp() {
        checkpointDataSource = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true) {
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
        }
        archivedSet = mutableSetOf()
    }

    private fun buildParams(
        isArchived: Boolean = false,
        fetchFullSnapshot: suspend () -> DeltaSyncBatch<String> = {
            DeltaSyncBatch(emptyList(), emptyList(), null, isServerConfirmed = true, isFullSnapshot = true)
        },
        fetchDeltaOnce: suspend (SyncWatermark) -> DeltaSyncBatch<String> = {
            DeltaSyncBatch(emptyList(), emptyList(), null, isServerConfirmed = true, isFullSnapshot = false)
        },
        observeDeltaFlow: (SyncWatermark?) -> Flow<DeltaSyncBatch<String>> = { emptyFlow() },
        replaceLocal: suspend (List<String>) -> Unit = {},
        applyDeltaLocal: suspend (List<String>, List<String>) -> Unit = { _, _ -> },
        getPendingIds: suspend () -> List<String> = { emptyList() },
        verifyOnServer: suspend (String) -> Boolean = { true },
        markSynced: suspend (String) -> Unit = {},
        verifyParentExists: (suspend () -> Boolean)? = { true },
        onAfterReconcile: (suspend () -> Unit)? = null,
        clockMillis: () -> Long = { nowMillis }
    ) = DeltaSyncParams(
        groupId = groupId,
        collectionName = collectionName,
        checkpointDataSource = checkpointDataSource,
        isGroupArchived = { isArchived },
        syncedArchivedGroupIds = archivedSet,
        fetchFullSnapshot = fetchFullSnapshot,
        fetchDeltaOnce = fetchDeltaOnce,
        observeDeltaFlow = observeDeltaFlow,
        replaceLocal = replaceLocal,
        applyDeltaLocal = applyDeltaLocal,
        getPendingIds = getPendingIds,
        verifyOnServer = verifyOnServer,
        markSynced = markSynced,
        entityLabel = "expense",
        logContext = "for group $groupId",
        performanceMonitor = performanceMonitor,
        verifyParentExists = verifyParentExists,
        onAfterReconcile = onAfterReconcile,
        clockMillis = clockMillis
    )

    @Test
    fun `first sync with null checkpoint runs full snapshot then attaches delta listener`() =
        runTest {
            val w1 = SyncWatermark(nowMillis / 1000L - 10L, 500)
            val observedWatermarks = mutableListOf<SyncWatermark?>()
            val replacedWith = mutableListOf<List<String>>()
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns null

            syncGroupCollectionDelta(
                buildParams(
                    fetchFullSnapshot = {
                        DeltaSyncBatch(
                            upserts = listOf("e1", "e2"),
                            deletedIds = emptyList(),
                            maxServerUpdatedAt = w1,
                            isServerConfirmed = true,
                            isFullSnapshot = true
                        )
                    },
                    observeDeltaFlow = { watermark ->
                        observedWatermarks.add(watermark)
                        emptyFlow()
                    },
                    replaceLocal = { replacedWith.add(it) }
                )
            )

            assertEquals(listOf(listOf("e1", "e2")), replacedWith)
            coVerify(exactly = 1) {
                checkpointDataSource.saveWatermark(groupId, collectionName, w1, nowMillis)
            }
            assertEquals(listOf(w1), observedWatermarks)
        }

    @Test
    fun `cold start with valid checkpoint skips full snapshot and attaches delta listener`() =
        runTest {
            val stored = SyncWatermark(nowMillis / 1000L - 3600L, 0)
            var fullSnapshotCalled = false
            val observedWatermarks = mutableListOf<SyncWatermark?>()
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

            syncGroupCollectionDelta(
                buildParams(
                    fetchFullSnapshot = {
                        fullSnapshotCalled = true
                        DeltaSyncBatch(emptyList(), emptyList(), null, true, true)
                    },
                    observeDeltaFlow = { watermark ->
                        observedWatermarks.add(watermark)
                        emptyFlow()
                    }
                )
            )

            assertFalse(fullSnapshotCalled)
            assertEquals(listOf(stored), observedWatermarks)
        }

    @Test
    fun `delta batch with upserts and deletedIds calls applyDeltaLocal and never replaceLocal`() = runTest {
        val stored = SyncWatermark(nowMillis / 1000L - 60L, 0)
        val next = SyncWatermark(nowMillis / 1000L, 100)
        var replaceCalled = false
        val applied = mutableListOf<Pair<List<String>, List<String>>>()
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = {
                    flowOf(
                        DeltaSyncBatch(
                            upserts = listOf("e-new"),
                            deletedIds = listOf("e-old"),
                            maxServerUpdatedAt = next,
                            isServerConfirmed = true,
                            isFullSnapshot = false
                        )
                    )
                },
                replaceLocal = { replaceCalled = true },
                applyDeltaLocal = { upserts, deleted -> applied.add(upserts to deleted) }
            )
        )

        assertFalse(replaceCalled)
        assertEquals(listOf(listOf("e-new") to listOf("e-old")), applied)
        coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, next, nowMillis) }
    }

    @Test
    fun `empty delta batch skips applyDeltaLocal write while still running confirmPendingSync`() = runTest {
        val stored = SyncWatermark(nowMillis / 1000L - 60L, 0)
        var applyCalled = false
        val marked = mutableListOf<String>()
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = {
                    flowOf(
                        DeltaSyncBatch(
                            upserts = emptyList(),
                            deletedIds = emptyList(),
                            maxServerUpdatedAt = null,
                            isServerConfirmed = true,
                            isFullSnapshot = false
                        )
                    )
                },
                applyDeltaLocal = { _, _ -> applyCalled = true },
                getPendingIds = { listOf("p1") },
                verifyOnServer = { true },
                markSynced = { marked.add(it) }
            )
        )

        assertFalse(applyCalled)
        assertEquals(listOf("p1"), marked)
    }

    @Test
    fun `watermark does not advance when snapshot is from cache`() = runTest {
        val stored = SyncWatermark(nowMillis / 1000L - 60L, 0)
        val cachedWatermark = SyncWatermark(nowMillis / 1000L, 0)
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = {
                    flowOf(
                        DeltaSyncBatch(
                            upserts = listOf("e1"),
                            deletedIds = emptyList(),
                            maxServerUpdatedAt = cachedWatermark,
                            isServerConfirmed = false,
                            isFullSnapshot = false
                        )
                    )
                }
            )
        )

        coVerify(exactly = 0) { checkpointDataSource.saveWatermark(any(), any(), any(), any()) }
    }

    @Test
    fun `watermark does not advance when snapshot has pending writes`() = runTest {
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns null
        val pendingWatermark = SyncWatermark(nowMillis / 1000L, 0)

        syncGroupCollectionDelta(
            buildParams(
                fetchFullSnapshot = {
                    DeltaSyncBatch(
                        upserts = listOf("e1"),
                        deletedIds = emptyList(),
                        maxServerUpdatedAt = pendingWatermark,
                        isServerConfirmed = false,
                        isFullSnapshot = true
                    )
                }
            )
        )

        coVerify(exactly = 0) { checkpointDataSource.saveWatermark(any(), any(), any(), any()) }
    }

    @Test
    fun `watermark advances monotonically and ignores out of order older watermarks`() = runTest {
        val stored = SyncWatermark(nowMillis / 1000L - 100L, 0)
        val older = SyncWatermark(nowMillis / 1000L - 200L, 0)
        val newer = SyncWatermark(nowMillis / 1000L - 50L, 0)
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = {
                    flowOf(
                        DeltaSyncBatch(
                            listOf("e1"),
                            emptyList(),
                            older,
                            isServerConfirmed = true,
                            isFullSnapshot = false
                        ),
                        DeltaSyncBatch(
                            listOf("e2"),
                            emptyList(),
                            newer,
                            isServerConfirmed = true,
                            isFullSnapshot = false
                        )
                    )
                }
            )
        )

        coVerify(exactly = 0) { checkpointDataSource.saveWatermark(groupId, collectionName, older, any()) }
        coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, newer, nowMillis) }
    }

    @Test
    fun `null maxServerUpdatedAt on empty or legacy collection does not write a synthetic client-clock watermark`() =
        runTest {
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns null

            syncGroupCollectionDelta(
                buildParams(
                    fetchFullSnapshot = {
                        DeltaSyncBatch(
                            emptyList(),
                            emptyList(),
                            maxServerUpdatedAt = null,
                            isServerConfirmed = true,
                            isFullSnapshot = true
                        )
                    },
                    observeDeltaFlow = {
                        flowOf(
                            DeltaSyncBatch(
                                listOf("legacy"),
                                emptyList(),
                                maxServerUpdatedAt = null,
                                isServerConfirmed = true,
                                isFullSnapshot = false
                            )
                        )
                    }
                )
            )

            coVerify(exactly = 0) { checkpointDataSource.saveWatermark(any(), any(), any(), any()) }
        }

    @Test
    fun `expired checkpoint older than 30 days clears checkpoint falls back to full snapshot and resets watermark`() =
        runTest {
            val thirtyOneDaysAgoSeconds = (nowMillis - TimeUnit.DAYS.toMillis(31L)) / 1000L
            val expiredWatermark = SyncWatermark(thirtyOneDaysAgoSeconds, 0)
            val freshWatermark = SyncWatermark(nowMillis / 1000L, 0)
            val replacedWith = mutableListOf<List<String>>()
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns expiredWatermark
            coEvery { checkpointDataSource.clearCheckpoint(groupId, collectionName) } just Runs

            syncGroupCollectionDelta(
                buildParams(
                    fetchFullSnapshot = {
                        DeltaSyncBatch(
                            upserts = listOf("fresh-1"),
                            deletedIds = emptyList(),
                            maxServerUpdatedAt = freshWatermark,
                            isServerConfirmed = true,
                            isFullSnapshot = true
                        )
                    },
                    replaceLocal = { replacedWith.add(it) }
                )
            )

            coVerifyOrder {
                checkpointDataSource.clearCheckpoint(groupId, collectionName)
                checkpointDataSource.saveWatermark(groupId, collectionName, freshWatermark, nowMillis)
            }
            assertEquals(listOf(listOf("fresh-1")), replacedWith)
        }

    @Test
    fun `clock skewed backwards is not falsely treated as expired`() = runTest {
        val futureWatermark = SyncWatermark((nowMillis / 1000L) + 3600L, 0)
        var fullSnapshotCalled = false
        val observedWatermarks = mutableListOf<SyncWatermark?>()
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns futureWatermark

        syncGroupCollectionDelta(
            buildParams(
                fetchFullSnapshot = {
                    fullSnapshotCalled = true
                    DeltaSyncBatch(emptyList(), emptyList(), null, true, true)
                },
                observeDeltaFlow = { watermark ->
                    observedWatermarks.add(watermark)
                    emptyFlow()
                }
            )
        )

        assertFalse(fullSnapshotCalled)
        assertEquals(listOf(futureWatermark), observedWatermarks)
        coVerify(exactly = 0) { checkpointDataSource.clearCheckpoint(any(), any()) }
    }

    @Test
    fun `delta batch hitting page limit advances cursor and re-subscribes from updated watermark`() = runTest {
        val w1 = SyncWatermark(nowMillis / 1000L - 100L, 0)
        val w2 = SyncWatermark(nowMillis / 1000L - 50L, 0)
        val w3 = SyncWatermark(nowMillis / 1000L, 0)
        val observedCursors = mutableListOf<SyncWatermark?>()
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns w1

        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = { cursor ->
                    observedCursors.add(cursor)
                    when (cursor) {
                        w1 -> flowOf(
                            DeltaSyncBatch(
                                upserts = listOf("batch-1"),
                                deletedIds = emptyList(),
                                maxServerUpdatedAt = w2,
                                isServerConfirmed = true,
                                isFullSnapshot = false,
                                reachedPageLimit = true
                            )
                        )
                        else -> flowOf(
                            DeltaSyncBatch(
                                upserts = listOf("batch-2"),
                                deletedIds = emptyList(),
                                maxServerUpdatedAt = w3,
                                isServerConfirmed = true,
                                isFullSnapshot = false,
                                reachedPageLimit = false
                            )
                        )
                    }
                }
            )
        )

        assertEquals(listOf(w1, w2), observedCursors)
        coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, w2, nowMillis) }
        coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, w3, nowMillis) }
    }

    @Test
    fun `archived group with valid checkpoint runs one-shot delta fetch without listener`() =
        runTest {
            val stored = SyncWatermark(nowMillis / 1000L - 120L, 0)
            val next = SyncWatermark(nowMillis / 1000L, 0)
            var listenerAttached = false
            val deltaCalls = mutableListOf<SyncWatermark>()
            val applied = mutableListOf<Pair<List<String>, List<String>>>()
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

            syncGroupCollectionDelta(
                buildParams(
                    isArchived = true,
                    fetchDeltaOnce = { watermark ->
                        deltaCalls.add(watermark)
                        DeltaSyncBatch(
                            listOf("u1"),
                            listOf("d1"),
                            next,
                            isServerConfirmed = true,
                            isFullSnapshot = false
                        )
                    },
                    observeDeltaFlow = {
                        listenerAttached = true
                        emptyFlow()
                    },
                    applyDeltaLocal = { u, d -> applied.add(u to d) }
                )
            )

            assertFalse(listenerAttached)
            assertEquals(listOf(stored), deltaCalls)
            assertEquals(listOf(listOf("u1") to listOf("d1")), applied)
            assertTrue(groupId in archivedSet)
            coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, next, nowMillis) }
        }

    @Test
    fun `archived group with null or expired checkpoint performs one-shot fetchFullSnapshot and persists watermark`() =
        runTest {
            val expiredSeconds = (nowMillis - TimeUnit.DAYS.toMillis(40L)) / 1000L
            val expired = SyncWatermark(expiredSeconds, 0)
            val fresh = SyncWatermark(nowMillis / 1000L, 0)
            val replaced = mutableListOf<List<String>>()
            coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns expired

            syncGroupCollectionDelta(
                buildParams(
                    isArchived = true,
                    fetchFullSnapshot = {
                        DeltaSyncBatch(
                            listOf("all-1"),
                            emptyList(),
                            fresh,
                            isServerConfirmed = true,
                            isFullSnapshot = true
                        )
                    },
                    replaceLocal = { replaced.add(it) }
                )
            )

            assertEquals(listOf(listOf("all-1")), replaced)
            assertTrue(groupId in archivedSet)
            coVerify(exactly = 1) { checkpointDataSource.clearCheckpoint(groupId, collectionName) }
            coVerify(exactly = 1) { checkpointDataSource.saveWatermark(groupId, collectionName, fresh, nowMillis) }
        }

    @Test
    fun `skips reconciliation and checkpoint save when verifyParentExists returns false`() = runTest {
        val w1 = SyncWatermark(nowMillis / 1000L, 0)
        var replaceCalled = false
        var applyCalled = false
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns null

        syncGroupCollectionDelta(
            buildParams(
                fetchFullSnapshot = {
                    DeltaSyncBatch(listOf("e1"), emptyList(), w1, isServerConfirmed = true, isFullSnapshot = true)
                },
                replaceLocal = { replaceCalled = true },
                applyDeltaLocal = { _, _ -> applyCalled = true },
                verifyParentExists = { false }
            )
        )

        assertFalse(replaceCalled)
        assertFalse(applyCalled)
        coVerify(exactly = 0) { checkpointDataSource.saveWatermark(any(), any(), any(), any()) }
    }

    @Test
    fun `rethrows CancellationException and catches non-cancellation exceptions gracefully`() = runTest {
        val stored = SyncWatermark(nowMillis / 1000L - 60L, 0)
        coEvery { checkpointDataSource.getWatermark(groupId, collectionName) } returns stored

        // Non-cancellation exception is swallowed gracefully
        syncGroupCollectionDelta(
            buildParams(
                observeDeltaFlow = { flow { throw IOException("Offline") } }
            )
        )

        // CancellationException is rethrown
        var cancellationRethrown = false
        try {
            syncGroupCollectionDelta(
                buildParams(
                    observeDeltaFlow = { flow { throw CancellationException("Cancelled") } }
                )
            )
        } catch (_: CancellationException) {
            cancellationRethrown = true
        }
        assertTrue(cancellationRethrown)
    }
}
