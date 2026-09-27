package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ContributionRepositoryImpl")
class ContributionRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cloudContributionDataSource: CloudContributionDataSource
    private lateinit var localContributionDataSource: LocalContributionDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var repository: ContributionRepositoryImpl

    private val testGroupId = "group-123"
    private val testUserId = "user-1"

    private val testContribution = Contribution(
        id = "contrib-1",
        groupId = testGroupId,
        userId = testUserId,
        amount = 30000L,
        currency = "EUR",
        createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
    )

    private val cloudContributions = listOf(
        Contribution(
            id = "cloud-1",
            groupId = testGroupId,
            userId = "user-1",
            amount = 10000L,
            currency = "EUR",
            createdAt = LocalDateTime.of(2026, 1, 10, 10, 0)
        ),
        Contribution(
            id = "cloud-2",
            groupId = testGroupId,
            userId = "user-2",
            amount = 20000L,
            currency = "EUR",
            createdAt = LocalDateTime.of(2026, 1, 11, 14, 0)
        )
    )

    private lateinit var localGroupDataSource:
        LocalGroupDataSource
    private lateinit var syncTeardownCoordinator: SyncTeardownCoordinator

    @BeforeEach
    fun setUp() {
        cloudContributionDataSource = mockk(relaxed = true)
        localContributionDataSource = mockk(relaxed = true)
        authenticationService = mockk()
        performanceMonitor = mockk(relaxed = true) {
            io.mockk.coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
            io.mockk.every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
        }

        coEvery { authenticationService.currentUserId() } returns testUserId

        localGroupDataSource = mockk(relaxed = true)
        syncTeardownCoordinator = mockk(relaxed = true)
        repository = ContributionRepositoryImpl(
            cloudContributionDataSource = cloudContributionDataSource,
            localContributionDataSource = localContributionDataSource,
            authenticationService = authenticationService,
            performanceMonitor = performanceMonitor,
            ioDispatcher = testDispatcher,
            localGroupDataSource = localGroupDataSource,
            syncTeardownCoordinator = syncTeardownCoordinator
        )
    }

    @Nested
    @DisplayName("AddContribution")
    inner class AddContribution {

        @Test
        fun `saves to local storage first`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)

            // Then - Local save should happen immediately
            coVerify(exactly = 1) { localContributionDataSource.saveContribution(any()) }
        }

        @Test
        fun `syncs to cloud in background`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs
            coEvery { cloudContributionDataSource.addContribution(any(), any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudContributionDataSource.addContribution(
                    testGroupId,
                    any()
                )
            }
        }

        @Test
        fun `generates UUID when id is blank`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(id = "", amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)

            // Then - Saved contribution should have a non-blank ID
            coVerify {
                localContributionDataSource.saveContribution(match { it.id.isNotBlank() })
            }
        }

        @Test
        fun `sets userId from authentication service when blank`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(userId = "", amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)

            // Then
            coVerify {
                localContributionDataSource.saveContribution(match { it.userId == testUserId })
            }
        }

        @Test
        fun `sets createdBy to current authenticated user`() = runTest(testDispatcher) {
            // Given — contribution has a different userId (impersonation scenario)
            val contribution = Contribution(
                userId = "target-user",
                amount = 5000L,
                currency = "EUR"
            )
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)

            // Then — createdBy is always the authenticated user (actor), not the target
            coVerify {
                localContributionDataSource.saveContribution(
                    match { it.createdBy == testUserId && it.userId == "target-user" }
                )
            }
        }

        @Test
        fun `cloud failure does not affect local save`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs
            coEvery {
                cloudContributionDataSource.addContribution(
                    any(),
                    any()
                )
            } throws RuntimeException("Network error")

            // When
            repository.addContribution(testGroupId, contribution)
            advanceUntilIdle()

            // Then - Local save should still have happened
            coVerify(exactly = 1) { localContributionDataSource.saveContribution(any()) }
        }

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs

            // When
            repository.addContribution(testGroupId, contribution)

            // Then
            coVerify {
                localContributionDataSource.saveContribution(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs
            coEvery { cloudContributionDataSource.addContribution(any(), any()) } just Runs
            coEvery {
                localContributionDataSource.updateSyncStatus(any(), any())
            } just Runs

            // When
            repository.addContribution(testGroupId, contribution)
            advanceUntilIdle()

            // Then
            coVerify {
                localContributionDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
        }

        @Test
        fun `updates to SYNC_FAILED after cloud sync failure`() = runTest(testDispatcher) {
            // Given
            val contribution = Contribution(amount = 5000L, currency = "EUR")
            coEvery { localContributionDataSource.saveContribution(any()) } just Runs
            coEvery {
                cloudContributionDataSource.addContribution(any(), any())
            } throws RuntimeException("Network error")
            coEvery {
                localContributionDataSource.updateSyncStatus(any(), any())
            } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localContributionDataSource.findContributionById(any())
            } returns testContribution.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.addContribution(testGroupId, contribution)
            advanceUntilIdle()

            // Then
            coVerify {
                localContributionDataSource.updateSyncStatus(
                    any(),
                    SyncStatus.SYNC_FAILED
                )
            }
        }
    }

    @Nested
    @DisplayName("GetGroupContributionsFlow")
    inner class GetGroupContributionsFlow {

        @Test
        fun `returns flow from local data source`() = runTest(testDispatcher) {
            // Given
            every {
                localContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(listOf(testContribution))
            every {
                cloudContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudContributions)
            coEvery {
                localContributionDataSource.replaceContributionsForGroup(any(), any())
            } just Runs

            // When
            val result = repository.getGroupContributionsFlow(testGroupId).first()

            // Then
            assertEquals(1, result.size)
            assertEquals(testContribution.id, result[0].id)
        }

        @Test
        fun `subscribes to cloud changes on start`() = runTest(testDispatcher) {
            // Given
            every {
                localContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudContributions)
            coEvery {
                localContributionDataSource.replaceContributionsForGroup(any(), any())
            } just Runs

            // When
            val flow = repository.getGroupContributionsFlow(testGroupId)
            flow.first()
            advanceUntilIdle()

            // Then
            coVerify {
                localContributionDataSource.replaceContributionsForGroup(
                    testGroupId,
                    cloudContributions
                )
            }
        }
    }

    @Nested
    @DisplayName("DeleteContribution")
    inner class DeleteContribution {

        @Test
        fun `deletes from local storage first`() = runTest(testDispatcher) {
            // Given
            val contributionId = "contrib-1"
            coEvery { localContributionDataSource.deleteContribution(contributionId) } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } just Runs

            // When
            repository.deleteContribution(testGroupId, contributionId)

            // Then
            coVerify(exactly = 1) {
                localContributionDataSource.deleteContribution(contributionId)
            }
        }

        @Test
        fun `syncs deletion to cloud in background`() = runTest(testDispatcher) {
            // Given
            val contributionId = "contrib-1"
            coEvery { localContributionDataSource.deleteContribution(contributionId) } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } just Runs

            // When
            repository.deleteContribution(testGroupId, contributionId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudContributionDataSource.deleteContribution(testGroupId, contributionId)
            }
        }

        @Test
        fun `cloud deletion failure does not affect local delete`() = runTest(testDispatcher) {
            // Given
            val contributionId = "contrib-1"
            coEvery { localContributionDataSource.deleteContribution(contributionId) } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.deleteContribution(testGroupId, contributionId)
            advanceUntilIdle()

            // Then - Local delete should still have happened
            coVerify(exactly = 1) {
                localContributionDataSource.deleteContribution(contributionId)
            }
        }

        @Test
        fun `always queues cloud deletion regardless of sync status`() = runTest(testDispatcher) {
            // Given — deleteContribution() no longer checks sync status; it always queues
            // the cloud deletion so Firestore SDK handles write ordering.
            val contributionId = "any-contrib"
            coEvery {
                localContributionDataSource.deleteContribution(contributionId)
            } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } just Runs

            // When
            repository.deleteContribution(testGroupId, contributionId)
            advanceUntilIdle()

            // Then — cloud deletion is always queued
            coVerify(exactly = 1) {
                cloudContributionDataSource.deleteContribution(testGroupId, contributionId)
            }
            coVerify(exactly = 1) {
                localContributionDataSource.deleteContribution(contributionId)
            }
        }

        @Test
        fun `syncs to cloud when contribution is SYNCED`() = runTest(testDispatcher) {
            // Given
            val contributionId = "synced-contrib"
            val syncedContribution = testContribution.copy(
                id = contributionId,
                syncStatus = SyncStatus.SYNCED
            )
            coEvery {
                localContributionDataSource.findContributionById(contributionId)
            } returns syncedContribution
            coEvery {
                localContributionDataSource.deleteContribution(contributionId)
            } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } just Runs

            // When
            repository.deleteContribution(testGroupId, contributionId)
            advanceUntilIdle()

            // Then — cloud deletion should happen
            coVerify(exactly = 1) {
                cloudContributionDataSource.deleteContribution(testGroupId, contributionId)
            }
        }

        @Test
        fun `syncs to cloud when contribution not found locally`() = runTest(testDispatcher) {
            // Given — contribution not found (null syncStatus != PENDING_SYNC)
            val contributionId = "unknown-contrib"
            coEvery {
                localContributionDataSource.findContributionById(contributionId)
            } returns null
            coEvery {
                localContributionDataSource.deleteContribution(contributionId)
            } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } just Runs

            // When
            repository.deleteContribution(testGroupId, contributionId)
            advanceUntilIdle()

            // Then — cloud deletion should still happen
            coVerify(exactly = 1) {
                cloudContributionDataSource.deleteContribution(testGroupId, contributionId)
            }
        }
    }

    @Nested
    @DisplayName("DeleteByLinkedExpenseId")
    inner class DeleteByLinkedExpenseId {

        private val linkedExpenseId = "expense-123"
        private val linkedContribution = Contribution(
            id = "contrib-linked",
            groupId = testGroupId,
            userId = testUserId,
            amount = 16500L,
            currency = "EUR",
            linkedExpenseId = linkedExpenseId,
            createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
        )

        @Test
        fun `deletes from local storage first`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns linkedContribution
            coEvery {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            } just Runs
            coEvery { cloudContributionDataSource.deleteContribution(any(), any()) } just Runs

            // When
            repository.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)

            // Then
            coVerify(exactly = 1) {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            }
        }

        @Test
        fun `syncs deletion to cloud in background`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns linkedContribution
            coEvery {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            } just Runs
            coEvery { cloudContributionDataSource.deleteContribution(any(), any()) } just Runs

            // When
            repository.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudContributionDataSource.deleteContribution(testGroupId, linkedContribution.id)
            }
        }

        @Test
        fun `handles not found gracefully`() = runTest(testDispatcher) {
            // Given - no linked contribution exists
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns null
            coEvery {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            } just Runs

            // When
            repository.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            advanceUntilIdle()

            // Then - Local delete still called (DAO handles no-match gracefully)
            coVerify(exactly = 1) {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            }
            // Cloud delete NOT called (no contribution found to sync)
            coVerify(exactly = 0) {
                cloudContributionDataSource.deleteContribution(any(), any())
            }
        }

        @Test
        fun `cloud failure does not affect local delete`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns linkedContribution
            coEvery {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            } just Runs
            coEvery {
                cloudContributionDataSource.deleteContribution(any(), any())
            } throws RuntimeException("Network error")

            // When
            repository.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            advanceUntilIdle()

            // Then - Local delete should still have happened
            coVerify(exactly = 1) {
                localContributionDataSource.deleteByLinkedExpenseId(testGroupId, linkedExpenseId)
            }
        }
    }

    @Nested
    @DisplayName("FindByLinkedExpenseId")
    inner class FindByLinkedExpenseId {

        private val linkedExpenseId = "expense-123"
        private val linkedContribution = Contribution(
            id = "contrib-linked",
            groupId = testGroupId,
            userId = testUserId,
            amount = 16500L,
            currency = "EUR",
            linkedExpenseId = linkedExpenseId,
            createdAt = LocalDateTime.of(2026, 1, 15, 12, 0)
        )

        @Test
        fun `returns contribution when found`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns linkedContribution

            // When
            val result = repository.findByLinkedExpenseId(testGroupId, linkedExpenseId)

            // Then
            assertEquals(linkedContribution.id, result?.id)
            assertEquals(linkedExpenseId, result?.linkedExpenseId)
        }

        @Test
        fun `returns null when not found`() = runTest(testDispatcher) {
            // Given
            coEvery {
                localContributionDataSource.findByLinkedExpenseId(testGroupId, linkedExpenseId)
            } returns null

            // When
            val result = repository.findByLinkedExpenseId(testGroupId, linkedExpenseId)

            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("ConfirmPendingSyncContributions")
    inner class ConfirmPendingSyncContributions {

        @Test
        fun `transitions PENDING_SYNC contributions to SYNCED when server confirms`() = runTest(testDispatcher) {
            // Given — cloud returns contributions, local has pending sync IDs
            every {
                localContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudContributions)
            coEvery {
                localContributionDataSource.replaceContributionsForGroup(any(), any())
            } just Runs
            coEvery {
                localContributionDataSource.getPendingSyncContributionIds(testGroupId)
            } returns listOf("pending-1")
            coEvery {
                cloudContributionDataSource.verifyContributionOnServer(testGroupId, "pending-1")
            } returns true
            coEvery { localContributionDataSource.updateSyncStatus(any(), any()) } just Runs

            // When — trigger the flow to start the cloud subscription
            repository.getGroupContributionsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — pending contribution should be confirmed as SYNCED
            coVerify {
                localContributionDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `keeps PENDING_SYNC when server verification fails`() = runTest(testDispatcher) {
            // Given
            every {
                localContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudContributions)
            coEvery {
                localContributionDataSource.replaceContributionsForGroup(any(), any())
            } just Runs
            coEvery {
                localContributionDataSource.getPendingSyncContributionIds(testGroupId)
            } returns listOf("pending-1")
            coEvery {
                cloudContributionDataSource.verifyContributionOnServer(testGroupId, "pending-1")
            } throws RuntimeException("Server unreachable")

            // When
            repository.getGroupContributionsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should NOT update sync status
            coVerify(exactly = 0) {
                localContributionDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `skips when no pending contributions exist`() = runTest(testDispatcher) {
            // Given
            every {
                localContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(emptyList())
            every {
                cloudContributionDataSource.getContributionsByGroupIdFlow(testGroupId)
            } returns flowOf(cloudContributions)
            coEvery {
                localContributionDataSource.replaceContributionsForGroup(any(), any())
            } just Runs
            coEvery {
                localContributionDataSource.getPendingSyncContributionIds(testGroupId)
            } returns emptyList()

            // When
            repository.getGroupContributionsFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — should not attempt any verification
            coVerify(exactly = 0) {
                cloudContributionDataSource.verifyContributionOnServer(any(), any())
            }
        }
    }
}
