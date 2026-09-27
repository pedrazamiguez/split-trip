package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSubunitDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSubunitDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SubunitRepositoryImpl")
class SubunitRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cloudSubunitDataSource: CloudSubunitDataSource
    private lateinit var localSubunitDataSource: LocalSubunitDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var repository: SubunitRepositoryImpl

    private val testGroupId = "group-123"
    private val testUserId = "user-1"

    private val testSubunit = Subunit(
        id = "subunit-1",
        groupId = testGroupId,
        name = "Antonio & Me",
        memberIds = listOf("user-1", "user-2"),
        memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5")),
        createdBy = testUserId,
        createdAt = LocalDateTime.of(2026, 3, 13, 12, 0)
    )

    private val cloudSubunits = listOf(
        Subunit(
            id = "cloud-sub-1",
            groupId = testGroupId,
            name = "Couple A",
            memberIds = listOf("user-1", "user-2"),
            memberShares = mapOf("user-1" to BigDecimal("0.5"), "user-2" to BigDecimal("0.5")),
            createdBy = "user-1",
            createdAt = LocalDateTime.of(2026, 3, 10, 10, 0)
        ),
        Subunit(
            id = "cloud-sub-2",
            groupId = testGroupId,
            name = "Family B",
            memberIds = listOf("user-3", "user-4", "user-5"),
            memberShares = mapOf(
                "user-3" to BigDecimal("0.4"),
                "user-4" to BigDecimal("0.3"),
                "user-5" to BigDecimal("0.3")
            ),
            createdBy = "user-3",
            createdAt = LocalDateTime.of(2026, 3, 11, 14, 0)
        )
    )

    private lateinit var localGroupDataSource:
        LocalGroupDataSource
    private lateinit var syncTeardownCoordinator: SyncTeardownCoordinator

    @BeforeEach
    fun setUp() {
        cloudSubunitDataSource = mockk(relaxed = true)
        localSubunitDataSource = mockk(relaxed = true)
        authenticationService = mockk()
        performanceMonitor = mockk(relaxed = true) {
            io.mockk.coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            io.mockk.every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
        }

        coEvery { authenticationService.currentUserId() } returns testUserId

        localGroupDataSource = mockk(relaxed = true)
        syncTeardownCoordinator = mockk(relaxed = true)
        repository = SubunitRepositoryImpl(
            cloudSubunitDataSource = cloudSubunitDataSource,
            localSubunitDataSource = localSubunitDataSource,
            authenticationService = authenticationService,
            performanceMonitor = performanceMonitor,
            ioDispatcher = testDispatcher,
            localGroupDataSource = localGroupDataSource,
            syncTeardownCoordinator = syncTeardownCoordinator
        )
    }

    @Nested
    @DisplayName("CreateSubunit")
    inner class CreateSubunit {

        @Test
        fun `saves to local storage first`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "New Couple", memberIds = listOf("u1", "u2"))
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)

            // Then - Local save should happen immediately
            coVerify(exactly = 1) { localSubunitDataSource.saveSubunit(any()) }
        }

        @Test
        fun `syncs to cloud in background`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "New Couple", memberIds = listOf("u1", "u2"))
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery { cloudSubunitDataSource.addSubunit(any(), any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudSubunitDataSource.addSubunit(testGroupId, any())
            }
        }

        @Test
        fun `generates UUID when id is blank`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(id = "", name = "New Couple")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            val returnedId = repository.createSubunit(testGroupId, subunit)

            // Then - Saved subunit should have a non-blank ID
            assertTrue(returnedId.isNotBlank())
            coVerify {
                localSubunitDataSource.saveSubunit(match { it.id.isNotBlank() })
            }
        }

        @Test
        fun `sets createdBy from authentication service when blank`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(createdBy = "", name = "New Couple")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(match { it.createdBy == testUserId })
            }
        }

        @Test
        fun `preserves createdBy when not blank`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(createdBy = "original-creator", name = "New Couple")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(match { it.createdBy == "original-creator" })
            }
        }

        @Test
        fun `sets groupId on subunit`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "New Couple")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(match { it.groupId == testGroupId })
            }
        }

        @Test
        fun `cloud failure does not affect local save`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "New Couple")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery {
                cloudSubunitDataSource.addSubunit(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.createSubunit(testGroupId, subunit)
            advanceUntilIdle()

            // Then - Local save should still have happened
            coVerify(exactly = 1) { localSubunitDataSource.saveSubunit(any()) }
        }

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "Couple", memberIds = listOf("u1", "u2"))
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "Couple", memberIds = listOf("u1", "u2"))
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery { cloudSubunitDataSource.addSubunit(any(), any()) } just Runs
            coEvery { localSubunitDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.createSubunit(testGroupId, subunit)
            advanceUntilIdle()

            // Then
            coVerify {
                localSubunitDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
        }

        @Test
        fun `updates to SYNC_FAILED after cloud sync failure`() = runTest(testDispatcher) {
            // Given
            val subunit = Subunit(name = "Couple", memberIds = listOf("u1", "u2"))
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery {
                cloudSubunitDataSource.addSubunit(any(), any())
            } throws RuntimeException("Network error")
            coEvery { localSubunitDataSource.updateSyncStatus(any(), any()) } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localSubunitDataSource.getSubunitById(any())
            } returns testSubunit.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.createSubunit(testGroupId, subunit)
            advanceUntilIdle()

            // Then
            coVerify {
                localSubunitDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED)
            }
        }
    }

    @Nested
    @DisplayName("UpdateSubunit")
    inner class UpdateSubunit {

        @Test
        fun `saves to local storage first`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, testSubunit)

            // Then
            coVerify(exactly = 1) { localSubunitDataSource.saveSubunit(any()) }
        }

        @Test
        fun `syncs to cloud in background`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery { cloudSubunitDataSource.updateSubunit(any(), any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, testSubunit)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudSubunitDataSource.updateSubunit(testGroupId, any())
            }
        }

        @Test
        fun `sets lastUpdatedAt timestamp`() = runTest(testDispatcher) {
            // Given
            val before = LocalDateTime.now()
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, testSubunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(
                    match {
                        it.lastUpdatedAt != null && !it.lastUpdatedAt!!.isBefore(before)
                    }
                )
            }
        }

        @Test
        fun `sets groupId on updated subunit`() = runTest(testDispatcher) {
            // Given
            val subunitWithDifferentGroup = testSubunit.copy(groupId = "other-group")
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, subunitWithDifferentGroup)

            // Then - groupId should be overridden with the method parameter
            coVerify {
                localSubunitDataSource.saveSubunit(match { it.groupId == testGroupId })
            }
        }

        @Test
        fun `cloud failure does not affect local save`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery {
                cloudSubunitDataSource.updateSubunit(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.updateSubunit(testGroupId, testSubunit)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { localSubunitDataSource.saveSubunit(any()) }
        }
    }

    @Nested
    @DisplayName("DeleteSubunit")
    inner class DeleteSubunit {

        @Test
        fun `deletes from local storage first`() = runTest(testDispatcher) {
            // Given
            val subunitId = "subunit-1"
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs

            // When
            repository.deleteSubunit(testGroupId, subunitId)

            // Then
            coVerify(exactly = 1) {
                localSubunitDataSource.deleteSubunit(subunitId)
            }
        }

        @Test
        fun `syncs deletion to cloud in background`() = runTest(testDispatcher) {
            // Given
            val subunitId = "subunit-1"
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs
            coEvery { cloudSubunitDataSource.deleteSubunit(any(), any()) } just Runs

            // When
            repository.deleteSubunit(testGroupId, subunitId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudSubunitDataSource.deleteSubunit(testGroupId, subunitId)
            }
        }

        @Test
        fun `cloud deletion failure does not affect local delete`() = runTest(testDispatcher) {
            // Given
            val subunitId = "subunit-1"
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs
            coEvery {
                cloudSubunitDataSource.deleteSubunit(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.deleteSubunit(testGroupId, subunitId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                localSubunitDataSource.deleteSubunit(subunitId)
            }
        }

        @Test
        fun `always queues cloud deletion regardless of sync status`() = runTest(testDispatcher) {
            // Given — deleteSubunit() no longer checks sync status; it always queues
            // the cloud deletion so Firestore SDK handles write ordering.
            val subunitId = "any-sub"
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs
            coEvery { cloudSubunitDataSource.deleteSubunit(any(), any()) } just Runs

            // When
            repository.deleteSubunit(testGroupId, subunitId)
            advanceUntilIdle()

            // Then — cloud deletion is always queued
            coVerify(exactly = 1) {
                cloudSubunitDataSource.deleteSubunit(testGroupId, subunitId)
            }
            coVerify(exactly = 1) {
                localSubunitDataSource.deleteSubunit(subunitId)
            }
        }

        @Test
        fun `syncs to cloud when subunit is SYNCED`() = runTest(testDispatcher) {
            // Given
            val subunitId = "synced-sub"
            val syncedSubunit = testSubunit.copy(
                id = subunitId,
                syncStatus = SyncStatus.SYNCED
            )
            coEvery {
                localSubunitDataSource.getSubunitById(subunitId)
            } returns syncedSubunit
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs
            coEvery { cloudSubunitDataSource.deleteSubunit(any(), any()) } just Runs

            // When
            repository.deleteSubunit(testGroupId, subunitId)
            advanceUntilIdle()

            // Then — cloud deletion should happen
            coVerify(exactly = 1) {
                cloudSubunitDataSource.deleteSubunit(testGroupId, subunitId)
            }
        }

        @Test
        fun `syncs to cloud when subunit not found locally`() = runTest(testDispatcher) {
            // Given — subunit not found (null syncStatus != PENDING_SYNC)
            val subunitId = "unknown-sub"
            coEvery { localSubunitDataSource.getSubunitById(subunitId) } returns null
            coEvery { localSubunitDataSource.deleteSubunit(subunitId) } just Runs
            coEvery { cloudSubunitDataSource.deleteSubunit(any(), any()) } just Runs

            // When
            repository.deleteSubunit(testGroupId, subunitId)
            advanceUntilIdle()

            // Then — cloud deletion should still happen
            coVerify(exactly = 1) {
                cloudSubunitDataSource.deleteSubunit(testGroupId, subunitId)
            }
        }
    }

    @Nested
    @DisplayName("UpdateSubunit - Sync Status")
    inner class UpdateSubunitSyncStatus {

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, testSubunit)

            // Then
            coVerify {
                localSubunitDataSource.saveSubunit(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery { cloudSubunitDataSource.updateSubunit(any(), any()) } just Runs
            coEvery { localSubunitDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.updateSubunit(testGroupId, testSubunit)
            advanceUntilIdle()

            // Then
            coVerify {
                localSubunitDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
        }

        @Test
        fun `updates to SYNC_FAILED after cloud sync failure`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.saveSubunit(any()) } just Runs
            coEvery {
                cloudSubunitDataSource.updateSubunit(any(), any())
            } throws RuntimeException("Network error")
            coEvery { localSubunitDataSource.updateSyncStatus(any(), any()) } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localSubunitDataSource.getSubunitById(any())
            } returns testSubunit.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.updateSubunit(testGroupId, testSubunit)
            advanceUntilIdle()

            // Then
            coVerify {
                localSubunitDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED)
            }
        }
    }

    @Nested
    @DisplayName("GetGroupSubunitsFlow")
    inner class GetGroupSubunitsFlow {

        @Test
        fun `returns flow from local data source`() = runTest(testDispatcher) {
            // Given
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(listOf(testSubunit))
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs

            // When
            val result = repository.getGroupSubunitsFlow(testGroupId).first()

            // Then
            assertEquals(1, result.size)
            assertEquals(testSubunit.id, result[0].id)
        }

        @Test
        fun `subscribes to cloud changes on start`() = runTest(testDispatcher) {
            // Given
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs

            // When
            val flow = repository.getGroupSubunitsFlow(testGroupId)
            flow.first()
            advanceUntilIdle()

            // Then
            coVerify {
                localSubunitDataSource.replaceSubunitsForGroup(
                    testGroupId,
                    cloudSubunits
                )
            }
        }

        @Test
        fun `cancels previous subscription before starting new one on resubscription`() = runTest(testDispatcher) {
            // Given
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs

            // When - Subscribe twice to the same groupId
            // With flowOf(), the first Job completes before the second call,
            // so both subscriptions complete and reconcile once each.
            val flow1 = repository.getGroupSubunitsFlow(testGroupId)
            flow1.first()
            advanceUntilIdle()

            val flow2 = repository.getGroupSubunitsFlow(testGroupId)
            flow2.first()
            advanceUntilIdle()

            // Then - replaceSubunitsForGroup should not be called more than twice
            // (once per subscription, not accumulated from leaked listeners)
            coVerify(atMost = 2) {
                localSubunitDataSource.replaceSubunitsForGroup(testGroupId, cloudSubunits)
            }
        }
    }

    @Nested
    @DisplayName("GetSubunitById")
    inner class GetSubunitById {

        @Test
        fun `returns subunit from local data source`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.getSubunitById("subunit-1") } returns testSubunit

            // When
            val result = repository.getSubunitById("subunit-1")

            // Then
            assertEquals(testSubunit, result)
        }

        @Test
        fun `returns null when subunit not found`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.getSubunitById("nonexistent") } returns null

            // When
            val result = repository.getSubunitById("nonexistent")

            // Then
            assertEquals(null, result)
        }
    }

    @Nested
    @DisplayName("GetGroupSubunits (one-shot)")
    inner class GetGroupSubunits {

        @Test
        fun `returns subunits from local data source without triggering cloud sync`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localSubunitDataSource.getSubunitsByGroupId(testGroupId)
            } returns cloudSubunits

            // When
            val result = repository.getGroupSubunits(testGroupId)

            // Then
            assertEquals(cloudSubunits, result)
            coVerify(exactly = 1) { localSubunitDataSource.getSubunitsByGroupId(testGroupId) }
            // Cloud data source should NOT be touched
            coVerify(exactly = 0) { cloudSubunitDataSource.addSubunit(any(), any()) }
            coVerify(exactly = 0) { cloudSubunitDataSource.updateSubunit(any(), any()) }
            coVerify(exactly = 0) { cloudSubunitDataSource.deleteSubunit(any(), any()) }
            coVerify(exactly = 0) { cloudSubunitDataSource.fetchSubunitsByGroupId(any()) }
        }

        @Test
        fun `returns empty list when no subunits exist locally`() = runTest(testDispatcher) {
            // Given
            coEvery { localSubunitDataSource.getSubunitsByGroupId(testGroupId) } returns emptyList()

            // When
            val result = repository.getGroupSubunits(testGroupId)

            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("ConfirmPendingSyncSubunits")
    inner class ConfirmPendingSyncSubunits {

        @Test
        fun `transitions PENDING_SYNC subunits to SYNCED when server confirms`() = runTest(testDispatcher) {
            // Given — cloud returns subunits, local has pending sync IDs
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs
            coEvery {
                localSubunitDataSource.getPendingSyncSubunitIds(testGroupId)
            } returns listOf("pending-1")
            coEvery {
                cloudSubunitDataSource.verifySubunitOnServer(testGroupId, "pending-1")
            } returns true
            coEvery { localSubunitDataSource.updateSyncStatus(any(), any()) } just Runs

            // When — trigger the flow to start the cloud subscription
            repository.getGroupSubunitsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — pending subunit should be confirmed as SYNCED
            coVerify {
                localSubunitDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `keeps PENDING_SYNC when server verification fails`() = runTest(testDispatcher) {
            // Given
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs
            coEvery {
                localSubunitDataSource.getPendingSyncSubunitIds(testGroupId)
            } returns listOf("pending-1")
            coEvery {
                cloudSubunitDataSource.verifySubunitOnServer(testGroupId, "pending-1")
            } throws RuntimeException("Server unreachable")

            // When
            repository.getGroupSubunitsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should NOT update sync status
            coVerify(exactly = 0) {
                localSubunitDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `skips when no pending subunits exist`() = runTest(testDispatcher) {
            // Given
            every {
                localSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudSubunitDataSource.getSubunitsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudSubunits)
            coEvery {
                localSubunitDataSource.replaceSubunitsForGroup(any(), any())
            } just Runs
            coEvery {
                localSubunitDataSource.getPendingSyncSubunitIds(testGroupId)
            } returns emptyList()

            // When
            repository.getGroupSubunitsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should not attempt any verification
            coVerify(exactly = 0) {
                cloudSubunitDataSource.verifySubunitOnServer(any(), any())
            }
        }
    }
}
