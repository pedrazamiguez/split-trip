package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.impl.GroupMembershipServiceImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupMembershipServiceTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var service: GroupMembershipService

    private val testUserId = "user-123"
    private val testGroupId = "group-456"

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        authenticationService = mockk()
        every { authenticationService.requireUserId() } returns testUserId
        service = GroupMembershipServiceImpl(groupRepository, authenticationService)
    }

    @Nested
    inner class RequireMembership {

        @Test
        fun `passes when user is a member of the group`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Test Group",
                members = listOf(testUserId, "user-other")
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then — no exception thrown
            service.requireMembership(testGroupId)
        }

        @Test
        fun `passes when user is the only member`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Solo Group",
                members = listOf(testUserId)
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then — no exception thrown
            service.requireMembership(testGroupId)
        }

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Other Group",
                members = listOf("user-other-1", "user-other-2")
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then
            try {
                service.requireMembership(testGroupId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertEquals(testGroupId, e.groupId)
                assertEquals(testUserId, e.userId)
            }
        }

        @Test
        fun `throws NotGroupMemberException when group has empty members list`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Empty Group",
                members = emptyList()
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then
            try {
                service.requireMembership(testGroupId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertEquals(testGroupId, e.groupId)
                assertEquals(testUserId, e.userId)
            }
        }

        @Test
        fun `throws NotGroupMemberException when group is not found`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroupId) } returns null

            // When / Then
            try {
                service.requireMembership(testGroupId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertEquals(testGroupId, e.groupId)
                assertEquals(testUserId, e.userId)
            }
        }

        @Test
        fun `throws IllegalStateException when user is not authenticated`() = runTest {
            // Given
            every { authenticationService.requireUserId() } throws IllegalStateException("Not authenticated")

            // When / Then
            try {
                service.requireMembership(testGroupId)
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("Not authenticated") == true)
            }
        }
    }

    @Nested
    inner class RequireUserInGroup {

        private val targetUserId = "target-user-789"

        @Test
        fun `passes when specified user is a member of the group`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Test Group",
                members = listOf(testUserId, targetUserId)
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then — no exception thrown
            service.requireUserInGroup(testGroupId, targetUserId)
        }

        @Test
        fun `throws NotGroupMemberException when specified user is not a member`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Other Group",
                members = listOf(testUserId, "user-other")
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When / Then
            try {
                service.requireUserInGroup(testGroupId, targetUserId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertEquals(testGroupId, e.groupId)
                assertEquals(targetUserId, e.userId)
            }
        }

        @Test
        fun `throws NotGroupMemberException when group is not found`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroupId) } returns null

            // When / Then
            try {
                service.requireUserInGroup(testGroupId, targetUserId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertEquals(testGroupId, e.groupId)
                assertEquals(targetUserId, e.userId)
            }
        }

        @Test
        fun `does not use authenticationService`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Test Group",
                members = listOf(targetUserId)
            )
            coEvery { groupRepository.getGroupById(testGroupId) } returns group

            // When
            service.requireUserInGroup(testGroupId, targetUserId)

            // Then — requireUserInGroup should NOT call authenticationService.requireUserId()
            io.mockk.verify(exactly = 0) { authenticationService.requireUserId() }
        }
    }
}
