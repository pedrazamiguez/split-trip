package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.worker.GroupDeletionRetryScheduler
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var cloudGroupDataSource: CloudGroupDataSource
    private lateinit var localGroupDataSource: LocalGroupDataSource
    private lateinit var authenticationService: AuthenticationService
    private lateinit var groupDeletionRetryScheduler: GroupDeletionRetryScheduler
    private lateinit var groupImageStorageService: GroupImageStorageService
    private lateinit var cloudStorageDataSource: CloudStorageDataSource
    private lateinit var repository: GroupRepositoryImpl

    private val testGroupId = "group-123"
    private val testGroup = Group(
        id = testGroupId,
        name = "Test Group",
        description = "A test group",
        currency = "EUR",
        extraCurrencies = emptyList(),
        members = listOf("user-1", "user-2", "user-3"),
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 0),
        lastUpdatedAt = LocalDateTime.of(2024, 1, 15, 12, 0)
    )

    @BeforeEach
    fun setUp() {
        cloudGroupDataSource = mockk(relaxed = true)
        localGroupDataSource = mockk(relaxed = true)
        authenticationService = mockk(relaxed = true)
        groupDeletionRetryScheduler = mockk(relaxed = true)
        groupImageStorageService = mockk(relaxed = true)
        cloudStorageDataSource = mockk(relaxed = true)

        every { authenticationService.requireUserId() } returns "current-user-id"

        repository = GroupRepositoryImpl(
            cloudGroupDataSource = cloudGroupDataSource,
            localGroupDataSource = localGroupDataSource,
            authenticationService = authenticationService,
            groupDeletionRetryScheduler = groupDeletionRetryScheduler,
            groupImageStorageService = groupImageStorageService,
            cloudStorageDataSource = cloudStorageDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @Nested
    inner class DeleteGroup {

        @Test
        fun `deletes group from local storage first`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)

            // Then
            coVerify(exactly = 1) { localGroupDataSource.deleteGroup(testGroupId) }
        }

        @Test
        fun `deletes local group cover image`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)

            // Then
            coVerify(exactly = 1) { groupImageStorageService.deleteLocalGroupImage(testGroupId) }
        }

        @Test
        fun `deletes remote group cover image in background`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudStorageDataSource.deleteGroupImage(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { cloudStorageDataSource.deleteGroupImage(testGroupId) }
        }

        @Test
        fun `always requests cloud deletion regardless of sync status`() = runTest(testDispatcher) {
            // Given — deleteGroup() no longer checks sync status; it always queues
            // the cloud deletion request so Firestore SDK handles write ordering.
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then — cloud deletion is always requested
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
            coVerify(exactly = 1) { localGroupDataSource.deleteGroup(testGroupId) }
        }

        @Test
        fun `requests cloud deletion when group is not PENDING_SYNC`() = runTest(testDispatcher) {
            // Given — group was previously synced (null syncStatus defaults to SYNCED behavior)
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns testGroup
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then — cloud deletion should be requested
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }

        @Test
        fun `requests cloud deletion when group is SYNCED`() = runTest(testDispatcher) {
            // Given
            val syncedGroup = testGroup.copy(syncStatus = SyncStatus.SYNCED)
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns syncedGroup
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then — cloud deletion should be requested
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }

        @Test
        fun `requests cloud deletion when group is SYNC_FAILED`() = runTest(testDispatcher) {
            // Given
            val failedGroup = testGroup.copy(syncStatus = SyncStatus.SYNC_FAILED)
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns failedGroup
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then — cloud deletion should be requested even for SYNC_FAILED
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }

        @Test
        fun `requests group deletion from cloud in background`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then - Cloud deletion request should be made in background
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }

        @Test
        fun `local delete completes even if cloud request fails`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } throws
                RuntimeException("Network error")

            // When - Should not throw
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then - Local delete should have completed
            coVerify(exactly = 1) { localGroupDataSource.deleteGroup(testGroupId) }
        }

        @Test
        fun `schedules WorkManager retry when cloud request fails`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } throws
                RuntimeException("Network error")

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then - Retry scheduler should be invoked with the group ID
            verify(exactly = 1) { groupDeletionRetryScheduler.scheduleRetry(testGroupId) }
        }

        @Test
        fun `does not schedule retry when cloud request succeeds`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then - Scheduler should NOT be called
            verify(exactly = 0) { groupDeletionRetryScheduler.scheduleRetry(any()) }
        }

        @Test
        fun `does not call old deleteGroup on cloud data source`() = runTest(testDispatcher) {
            // Given
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then - Old deleteGroup should NOT be called; only requestGroupDeletion
            coVerify(exactly = 0) { cloudGroupDataSource.deleteGroup(any()) }
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }

        @Test
        fun `requests cloud deletion when group not found locally`() = runTest(testDispatcher) {
            // Given — group is not found locally (already deleted, or race condition)
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns null
            coEvery { localGroupDataSource.deleteGroup(testGroupId) } just Runs
            coEvery { cloudGroupDataSource.requestGroupDeletion(testGroupId) } just Runs

            // When
            repository.deleteGroup(testGroupId)
            advanceUntilIdle()

            // Then — null syncStatus != PENDING_SYNC, so cloud deletion is requested
            coVerify(exactly = 1) { cloudGroupDataSource.requestGroupDeletion(testGroupId) }
        }
    }

    @Nested
    inner class GetAllGroupsFlow {

        @Test
        fun `returns flow from local data source`() = runTest(testDispatcher) {
            // Given
            val groups = listOf(testGroup)
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(groups)
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(emptyList())
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs

            // When
            val result = repository.getAllGroupsFlow().first()

            // Then
            assertEquals(groups, result)
        }

        @Test
        fun `returns groups with members from local data source`() = runTest(testDispatcher) {
            // Given
            val groupWithMembers = testGroup.copy(
                members = listOf("user-1", "user-2", "user-3")
            )
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(listOf(groupWithMembers))
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(emptyList())
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs

            // When
            var emittedGroups: List<Group>? = null
            repository.getAllGroupsFlow().collect { groups ->
                emittedGroups = groups
            }

            // Then
            assertEquals(3, emittedGroups?.first()?.members?.size)
            assertEquals(listOf("user-1", "user-2", "user-3"), emittedGroups?.first()?.members)
        }

        @Test
        fun `subscribes to real-time cloud changes and replaces local`() = runTest(testDispatcher) {
            // Given
            val cloudGroups = listOf(
                testGroup.copy(members = listOf("member-a", "member-b"))
            )
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(emptyList())
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(cloudGroups)
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs

            // When
            repository.getAllGroupsFlow().first()
            advanceUntilIdle()

            // Then - Verify groups replaced in local (not upserted) to handle deletions
            coVerify {
                localGroupDataSource.replaceAllGroups(
                    match { groups ->
                        groups.any { it.members == listOf("member-a", "member-b") }
                    }
                )
            }
        }

        @Test
        fun `cloud sync failure does not affect local data flow`() = runTest(testDispatcher) {
            // Given
            val localGroups = listOf(testGroup)
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(localGroups)
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flow {
                throw IOException("Network error")
            }

            // When
            val result = repository.getAllGroupsFlow().first()
            advanceUntilIdle()

            // Then - Should still return local data
            assertEquals(1, result.size)
            assertEquals(testGroup.id, result[0].id)
        }
    }

    @Nested
    inner class GetGroupById {

        @Test
        fun `returns group with members from local data source`() = runTest(testDispatcher) {
            // Given
            val groupWithMembers = testGroup.copy(
                members = listOf("user-1", "user-2")
            )
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns groupWithMembers

            // When
            val result = repository.getGroupById(testGroupId)

            // Then
            assertEquals(2, result?.members?.size)
            assertEquals(listOf("user-1", "user-2"), result?.members)
        }

        @Test
        fun `falls back to cloud and preserves members when not found locally`() = runTest(testDispatcher) {
            // Given
            val cloudGroup = testGroup.copy(
                members = listOf("cloud-user-1", "cloud-user-2", "cloud-user-3")
            )
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns null
            coEvery { cloudGroupDataSource.getGroupById(testGroupId) } returns cloudGroup
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs

            // When
            val result = repository.getGroupById(testGroupId)

            // Then
            assertEquals(3, result?.members?.size)
            assertEquals(listOf("cloud-user-1", "cloud-user-2", "cloud-user-3"), result?.members)

            // Also verify it's cached to local
            coVerify {
                localGroupDataSource.saveGroup(
                    match { group ->
                        group.members == listOf("cloud-user-1", "cloud-user-2", "cloud-user-3")
                    }
                )
            }
        }

        @Test
        fun `handles group with no members`() = runTest(testDispatcher) {
            // Given
            val groupWithoutMembers = testGroup.copy(members = emptyList())
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns groupWithoutMembers

            // When
            val result = repository.getGroupById(testGroupId)

            // Then
            assertEquals(emptyList<String>(), result?.members)
        }
    }

    @Nested
    inner class CreateGroup {

        @Test
        fun `saves to local first then syncs to cloud`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then - Local save should happen
            coVerify(exactly = 1) { localGroupDataSource.saveGroup(any()) }
        }

        @Test
        fun `saves with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs

            // When
            repository.createGroup(newGroup)

            // Then
            coVerify {
                localGroupDataSource.saveGroup(
                    match { it.syncStatus == SyncStatus.PENDING_SYNC }
                )
            }
        }

        @Test
        fun `commits local temp image synchronously when mainImagePath is present`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "", mainImagePath = "file:///temp/image.webp")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { groupImageStorageService.commitGroupImage(any(), "file:///temp/image.webp") } returns
                "file:///permanent/image.webp"

            // When
            repository.createGroup(newGroup)

            // Then
            coVerify(exactly = 1) { groupImageStorageService.commitGroupImage(any(), "file:///temp/image.webp") }
            coVerify {
                localGroupDataSource.saveGroup(
                    match { it.mainImagePath == "file:///permanent/image.webp" }
                )
            }
        }

        @Test
        fun `uploads image to cloud in background when committed local image is present`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "", mainImagePath = "file:///temp/image.webp")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { groupImageStorageService.commitGroupImage(any(), "file:///temp/image.webp") } returns
                "file:///permanent/image.webp"
            coEvery {
                cloudStorageDataSource.uploadGroupImage(any(), "file:///permanent/image.webp", "image/webp")
            } returns
                "https://firebase.storage/image.webp"

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudStorageDataSource.uploadGroupImage(any(), "file:///permanent/image.webp", "image/webp")
            }
            coVerify {
                localGroupDataSource.saveGroup(
                    match { it.mainImagePath == "https://firebase.storage/image.webp" }
                )
                cloudGroupDataSource.createGroup(
                    match { it.mainImagePath == "https://firebase.storage/image.webp" }
                )
            }
        }

        @Test
        fun `continues group creation sync even if cloud image upload fails`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "", mainImagePath = "file:///temp/image.webp")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { groupImageStorageService.commitGroupImage(any(), "file:///temp/image.webp") } returns
                "file:///permanent/image.webp"
            coEvery { cloudStorageDataSource.uploadGroupImage(any(), any(), any()) } throws
                RuntimeException("Upload failed")
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                cloudGroupDataSource.createGroup(
                    match {
                        it.mainImagePath ==
                            "file:///permanent/image.webp"
                    }
                )
            }
        }

        @Test
        fun `updates to SYNCED after successful cloud sync and server verification`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"
            coEvery { cloudGroupDataSource.verifyGroupOnServer(any()) } returns true
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then
            coVerify {
                localGroupDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
        }

        @Test
        fun `stays PENDING_SYNC when server verification fails (offline)`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"
            coEvery {
                cloudGroupDataSource.verifyGroupOnServer(any())
            } throws RuntimeException("Server unreachable")
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then — should NOT update to SYNCED or SYNC_FAILED
            coVerify(exactly = 0) {
                localGroupDataSource.updateSyncStatus(any(), SyncStatus.SYNCED)
            }
            coVerify(exactly = 0) {
                localGroupDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED)
            }
        }

        @Test
        fun `updates to SYNC_FAILED when cloud write fails`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery {
                cloudGroupDataSource.createGroup(any())
            } throws RuntimeException("Permission denied")
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs
            // Status guard: entity is still PENDING_SYNC so SYNC_FAILED is allowed
            coEvery {
                localGroupDataSource.getGroupById(any())
            } returns testGroup.copy(syncStatus = SyncStatus.PENDING_SYNC)

            // When
            repository.createGroup(newGroup)
            advanceUntilIdle()

            // Then
            coVerify {
                localGroupDataSource.updateSyncStatus(any(), SyncStatus.SYNC_FAILED)
            }
            // Verify server verification was NOT attempted after write failure
            coVerify(exactly = 0) {
                cloudGroupDataSource.verifyGroupOnServer(any())
            }
        }

        @Test
        fun `adds current user to members list when not already included`() = runTest(testDispatcher) {
            // Given — members list does NOT include the current user
            val newGroup = testGroup.copy(
                id = "",
                members = listOf("other-user-1", "other-user-2")
            )
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"
            coEvery { cloudGroupDataSource.verifyGroupOnServer(any()) } returns true
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.createGroup(newGroup)

            // Then — creator should be appended to members
            coVerify {
                localGroupDataSource.saveGroup(
                    match {
                        "current-user-id" in it.members &&
                            "other-user-1" in it.members &&
                            "other-user-2" in it.members &&
                            it.members.size == 3
                    }
                )
            }
        }

        @Test
        fun `does not duplicate current user in members list when already included`() = runTest(testDispatcher) {
            // Given — members list already includes the current user
            val newGroup = testGroup.copy(
                id = "",
                members = listOf("current-user-id", "other-user-1")
            )
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.createGroup(any()) } returns "new-id"
            coEvery { cloudGroupDataSource.verifyGroupOnServer(any()) } returns true
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs

            // When
            repository.createGroup(newGroup)

            // Then — should keep original members (no duplicate)
            coVerify {
                localGroupDataSource.saveGroup(
                    match {
                        it.members.size == 2 &&
                            it.members.count { m -> m == "current-user-id" } == 1
                    }
                )
            }
        }

        @Test
        fun `generates UUID for group ID`() = runTest(testDispatcher) {
            // Given
            val newGroup = testGroup.copy(id = "")
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs

            // When
            val returnedId = repository.createGroup(newGroup)

            // Then — should generate a non-empty UUID
            assertNotEquals("", returnedId)
            coVerify {
                localGroupDataSource.saveGroup(match { it.id.isNotBlank() })
            }
        }
    }

    @Nested
    inner class ConfirmPendingSyncGroups {

        @Test
        fun `transitions PENDING_SYNC groups to SYNCED when server confirms`() = runTest(testDispatcher) {
            // Given — cloud returns groups, local has pending sync IDs
            val remoteGroups = listOf(testGroup)
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(emptyList())
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(remoteGroups)
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs
            coEvery { localGroupDataSource.getPendingSyncGroupIds() } returns listOf("pending-1")
            coEvery { cloudGroupDataSource.verifyGroupOnServer("pending-1") } returns true
            coEvery { localGroupDataSource.updateSyncStatus(any(), any()) } just Runs

            // When — trigger the flow to start the cloud subscription
            repository.getAllGroupsFlow().first()
            advanceUntilIdle()

            // Then — pending group should be confirmed as SYNCED
            coVerify { localGroupDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED) }
        }

        @Test
        fun `keeps PENDING_SYNC when server verification fails`() = runTest(testDispatcher) {
            // Given
            val remoteGroups = listOf(testGroup)
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(emptyList())
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(remoteGroups)
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs
            coEvery { localGroupDataSource.getPendingSyncGroupIds() } returns listOf("pending-1")
            coEvery {
                cloudGroupDataSource.verifyGroupOnServer("pending-1")
            } throws RuntimeException("Server unreachable")

            // When
            repository.getAllGroupsFlow().first()
            advanceUntilIdle()

            // Then — should NOT update sync status
            coVerify(exactly = 0) {
                localGroupDataSource.updateSyncStatus("pending-1", SyncStatus.SYNCED)
            }
        }

        @Test
        fun `skips when no pending groups exist`() = runTest(testDispatcher) {
            // Given
            val remoteGroups = listOf(testGroup)
            every { localGroupDataSource.getGroupsFlow() } returns flowOf(emptyList())
            every { cloudGroupDataSource.getAllGroupsFlow() } returns flowOf(remoteGroups)
            coEvery { localGroupDataSource.replaceAllGroups(any()) } just Runs
            coEvery { localGroupDataSource.getPendingSyncGroupIds() } returns emptyList()

            // When
            repository.getAllGroupsFlow().first()
            advanceUntilIdle()

            // Then — should not attempt any verification
            coVerify(exactly = 0) { cloudGroupDataSource.verifyGroupOnServer(any()) }
        }
    }

    @Nested
    inner class ReconcileUnregisteredUser {

        @Test
        fun `reconciles locally first, then reconciles cloud`() = runTest(testDispatcher) {
            // Given
            val pendingUserId = "pending-uid-123"
            val activeUserId = "active-uid-456"
            coEvery { localGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) } just Runs
            coEvery { cloudGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) } just Runs

            // When
            repository.reconcileUnregisteredUser(pendingUserId, activeUserId)

            // Then
            coVerify(exactly = 1) { localGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) }
            coVerify(exactly = 1) { cloudGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) }
        }

        @Test
        fun `propagates exception when cloud reconciliation fails`() = runTest(testDispatcher) {
            // Given
            val pendingUserId = "pending-uid-123"
            val activeUserId = "active-uid-456"
            val expectedException = RuntimeException("Cloud failed")
            coEvery { localGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) } just Runs
            coEvery { cloudGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) } throws
                expectedException

            // When
            val result = runCatching {
                repository.reconcileUnregisteredUser(pendingUserId, activeUserId)
            }

            // Then
            org.junit.jupiter.api.Assertions.assertTrue(result.isFailure)
            assertEquals(expectedException, result.exceptionOrNull())
            coVerify(exactly = 1) { localGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) }
            coVerify(exactly = 1) { cloudGroupDataSource.reconcileUnregisteredUser(pendingUserId, activeUserId) }
        }
    }

    @Nested
    inner class UpdateGroup {

        @Test
        fun `saves group to local storage with PENDING_SYNC status`() = runTest(testDispatcher) {
            // Given
            val initialGroup = testGroup.copy(mainImagePath = "old-path")
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns initialGroup
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs

            // When
            repository.updateGroup(testGroup)

            // Then
            coVerify(exactly = 1) {
                localGroupDataSource.saveGroup(
                    match {
                        it.id == testGroupId && it.syncStatus == SyncStatus.PENDING_SYNC
                    }
                )
            }
        }

        @Test
        fun `launches background job to sync to cloud`() = runTest(testDispatcher) {
            // Given
            val initialGroup = testGroup.copy(mainImagePath = "old-path")
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns initialGroup
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.updateGroup(any()) } just Runs

            // When
            repository.updateGroup(testGroup)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { cloudGroupDataSource.updateGroup(match { it.id == testGroupId }) }
        }

        @Test
        fun `deletes local image when cover photo is removed`() = runTest(testDispatcher) {
            // Given
            val initialGroup = testGroup.copy(mainImagePath = "old-path")
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns initialGroup
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { groupImageStorageService.deleteLocalGroupImage(testGroupId) } just Runs

            // When
            repository.updateGroup(testGroup.copy(mainImagePath = null))

            // Then
            coVerify(exactly = 1) { groupImageStorageService.deleteLocalGroupImage(testGroupId) }
        }

        @Test
        fun `uploads new image when cover photo changes`() = runTest(testDispatcher) {
            // Given
            val initialGroup = testGroup.copy(mainImagePath = "old-path")
            val newLocalPath = "new-temp-path"
            val uploadedUrl = "https://example.com/new.webp"
            coEvery { localGroupDataSource.getGroupById(testGroupId) } returns initialGroup
            coEvery { groupImageStorageService.commitGroupImage(testGroupId, newLocalPath) } returns "new-local-path"
            coEvery { cloudStorageDataSource.uploadGroupImage(testGroupId, "new-local-path", "image/webp") } returns
                uploadedUrl
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs
            coEvery { cloudGroupDataSource.updateGroup(any()) } just Runs

            // When
            repository.updateGroup(testGroup.copy(mainImagePath = newLocalPath))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { groupImageStorageService.commitGroupImage(testGroupId, newLocalPath) }
            coVerify(exactly = 1) {
                cloudStorageDataSource.uploadGroupImage(testGroupId, "new-local-path", "image/webp")
            }
            coVerify { cloudGroupDataSource.updateGroup(match { it.mainImagePath == uploadedUrl }) }
        }
    }

    @Nested
    inner class GetGroupByIdFlow {

        @Test
        fun `emits group from local data source`() = runTest(testDispatcher) {
            // Given
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(testGroup)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flowOf(null)

            // When
            val result = repository.getGroupByIdFlow(testGroupId).first()

            // Then
            assertEquals(testGroup, result)
        }

        @Test
        fun `subscribes to single-group cloud flow on start`() = runTest(testDispatcher) {
            // Given
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(null)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flowOf(null)

            // When
            repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { cloudGroupDataSource.getGroupFlow(testGroupId) }
        }

        @Test
        fun `saves cloud group to local when cloud emits an update`() = runTest(testDispatcher) {
            // Given
            val updatedGroup = testGroup.copy(status = es.pedrazamiguez.splittrip.domain.enums.GroupStatus.ARCHIVED)
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(testGroup)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flowOf(updatedGroup)
            coEvery { localGroupDataSource.saveGroup(any()) } just Runs

            // When
            repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { localGroupDataSource.saveGroup(updatedGroup) }
        }

        @Test
        fun `handles cloud flow error gracefully`() = runTest(testDispatcher) {
            // Given
            val localGroup = testGroup
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(localGroup)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flow {
                throw java.io.IOException("Network error")
            }

            // When
            val result = repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — local flow emission is unaffected
            assertEquals(localGroup, result)
        }

        @Test
        fun `does not save null group to local when cloud emits null`() = runTest(testDispatcher) {
            // Given
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(testGroup)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flowOf(null)

            // When
            repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — saveGroup should not be called
            coVerify(exactly = 0) { localGroupDataSource.saveGroup(any()) }
        }

        @Test
        fun `cancels previous subscription when flow is re-collected for same id`() = runTest(testDispatcher) {
            // Given
            every { localGroupDataSource.getGroupByIdFlow(testGroupId) } returns flowOf(testGroup)
            every { cloudGroupDataSource.getGroupFlow(testGroupId) } returns flowOf(null)

            // When — collect twice, advance between each to let coroutines start
            repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()
            repository.getGroupByIdFlow(testGroupId).first()
            advanceUntilIdle()

            // Then — getGroupFlow should be called twice (once per collect,
            // with old subscription cancelled before new one starts)
            coVerify(exactly = 2) { cloudGroupDataSource.getGroupFlow(testGroupId) }
        }
    }
}
