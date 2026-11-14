package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.RemoveGroupMemberUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RemoveGroupMemberUseCaseImplTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var getNonRemovableGroupMembersUseCase: GetNonRemovableGroupMembersUseCase
    private lateinit var useCase: RemoveGroupMemberUseCase

    private val groupId = "group-123"
    private val creatorId = "user-creator"
    private val memberToRemove = "user-to-remove"
    private val otherMember = "user-other"

    private val testGroup = Group(
        id = groupId,
        name = "Test Group",
        members = listOf(creatorId, memberToRemove, otherMember),
        createdBy = creatorId,
        status = GroupStatus.ACTIVE
    )

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        getNonRemovableGroupMembersUseCase = mockk()

        useCase = RemoveGroupMemberUseCaseImpl(
            groupRepository = groupRepository,
            getNonRemovableGroupMembersUseCase = getNonRemovableGroupMembersUseCase
        )

        coEvery { groupRepository.getGroupById(groupId) } returns testGroup
        coEvery { groupRepository.removeMember(any(), any()) } just Runs
        coEvery { getNonRemovableGroupMembersUseCase(groupId) } returns emptyMap()
    }

    @Test
    fun `removes member when group is active, user is member, and not in nonRemovable map`() = runTest {
        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { groupRepository.removeMember(groupId, memberToRemove) }
    }

    @Test
    fun `fails when group not found`() = runTest {
        coEvery { groupRepository.getGroupById(groupId) } returns null

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `fails with GroupArchivedException when group is archived`() = runTest {
        coEvery { groupRepository.getGroupById(groupId) } returns testGroup.copy(status = GroupStatus.ARCHIVED)

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GroupArchivedException)
    }

    @Test
    fun `fails with NOT_A_MEMBER when user is not in group`() = runTest {
        val result = useCase(groupId, "non-member")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as CannotRemoveMemberException
        assertEquals(CannotRemoveMemberException.Reason.NOT_A_MEMBER, exception.reason)
    }

    @Test
    fun `fails with IS_CREATOR when getNonRemovableGroupMembersUseCase returns IS_CREATOR`() = runTest {
        coEvery {
            getNonRemovableGroupMembersUseCase(groupId)
        } returns mapOf(creatorId to CannotRemoveMemberException.Reason.IS_CREATOR)

        val result = useCase(groupId, creatorId)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as CannotRemoveMemberException
        assertEquals(CannotRemoveMemberException.Reason.IS_CREATOR, exception.reason)
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }

    @Test
    fun `fails with NON_ZERO_BALANCE when getNonRemovableGroupMembersUseCase returns NON_ZERO_BALANCE`() = runTest {
        coEvery {
            getNonRemovableGroupMembersUseCase(groupId)
        } returns mapOf(memberToRemove to CannotRemoveMemberException.Reason.NON_ZERO_BALANCE)

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as CannotRemoveMemberException
        assertEquals(CannotRemoveMemberException.Reason.NON_ZERO_BALANCE, exception.reason)
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }

    @Test
    fun `fails with LAST_MEMBER when getNonRemovableGroupMembersUseCase returns LAST_MEMBER`() = runTest {
        coEvery {
            getNonRemovableGroupMembersUseCase(groupId)
        } returns mapOf(memberToRemove to CannotRemoveMemberException.Reason.LAST_MEMBER)

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as CannotRemoveMemberException
        assertEquals(CannotRemoveMemberException.Reason.LAST_MEMBER, exception.reason)
        coVerify(exactly = 0) { groupRepository.removeMember(any(), any()) }
    }
}
