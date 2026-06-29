package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteContributionUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeleteContributionUseCaseTest {

    private lateinit var contributionRepository: ContributionRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: DeleteContributionUseCase

    @BeforeEach
    fun setUp() {
        contributionRepository = mockk()
        groupMembershipService = mockk()
        groupRepository = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        coEvery { groupRepository.getGroupById(any()) } returns mockk {
            every { status } returns GroupStatus.ACTIVE
        }

        useCase = DeleteContributionUseCaseImpl(
            contributionRepository = contributionRepository,
            groupMembershipService = groupMembershipService,
            groupRepository = groupRepository
        )
    }

    // ── Group Archived validation ─────────────────────────────────────────────

    @Nested
    inner class GroupArchivedValidation {

        @Test
        fun `throws GroupArchivedException when group is archived`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            coEvery { groupRepository.getGroupById(groupId) } returns mockk {
                every { status } returns GroupStatus.ARCHIVED
            }

            // When / Then
            assertThrows<GroupArchivedException> {
                useCase(groupId, contributionId)
            }
        }
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When / Then
            val exception = assertThrows<NotGroupMemberException> {
                useCase(groupId, contributionId)
            }
            assertTrue(exception.groupId == groupId)
        }

        @Test
        fun `does not delete contribution when membership check fails`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            runCatching { useCase(groupId, contributionId) }

            // Then
            coVerify(exactly = 0) { contributionRepository.deleteContribution(any(), any()) }
        }

        @Test
        fun `calls requireMembership before deleting`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            coEvery { contributionRepository.deleteContribution(groupId, contributionId) } just Runs

            // When
            useCase(groupId, contributionId)

            // Then
            coVerifyOrder {
                groupMembershipService.requireMembership(groupId)
                contributionRepository.deleteContribution(groupId, contributionId)
            }
        }
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    inner class Invocation {

        @Test
        fun `delegates to repository deleteContribution`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            coEvery { contributionRepository.deleteContribution(groupId, contributionId) } just Runs

            // When
            useCase(groupId, contributionId)

            // Then
            coVerify(exactly = 1) { contributionRepository.deleteContribution(groupId, contributionId) }
        }

        @Test
        fun `passes correct groupId and contributionId to repository`() = runTest {
            // Given
            val groupId = "specific-group-id-789"
            val contributionId = "specific-contrib-id-012"
            coEvery { contributionRepository.deleteContribution(any(), any()) } just Runs

            // When
            useCase(groupId, contributionId)

            // Then
            coVerify { contributionRepository.deleteContribution(groupId, contributionId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            val groupId = "group-123"
            val contributionId = "contrib-456"
            val exception = RuntimeException("Delete failed")
            coEvery { contributionRepository.deleteContribution(groupId, contributionId) } throws exception

            // When / Then
            val thrownException = assertThrows<RuntimeException> {
                useCase(groupId, contributionId)
            }
            assertTrue(thrownException.message == "Delete failed")
        }
    }
}
