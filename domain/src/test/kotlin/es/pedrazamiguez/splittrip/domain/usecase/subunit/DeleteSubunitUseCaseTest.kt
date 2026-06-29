package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.DeleteSubunitUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("DeleteSubunitUseCase")
class DeleteSubunitUseCaseTest {

    private lateinit var subunitRepository: SubunitRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: DeleteSubunitUseCase

    private val groupId = "group-123"
    private val subunitId = "subunit-456"

    @BeforeEach
    fun setUp() {
        subunitRepository = mockk(relaxed = true)
        groupMembershipService = mockk()
        groupRepository = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        coEvery { groupRepository.getGroupById(any()) } returns mockk {
            every { status } returns GroupStatus.ACTIVE
        }

        useCase = DeleteSubunitUseCaseImpl(
            subunitRepository = subunitRepository,
            groupMembershipService = groupMembershipService,
            groupRepository = groupRepository
        )
    }

    // ── Group Archived validation ─────────────────────────────────────────────

    @Nested
    @DisplayName("Group Archived validation")
    inner class GroupArchivedValidation {

        @Test
        fun `fails when group is archived`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns mockk {
                every { status } returns GroupStatus.ARCHIVED
            }

            assertThrows<GroupArchivedException> {
                useCase(groupId, subunitId)
            }
        }
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Membership validation")
    inner class MembershipValidation {

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When / Then
            val exception = assertThrows<NotGroupMemberException> {
                useCase(groupId, subunitId)
            }
            assertTrue(exception.groupId == groupId)
        }

        @Test
        fun `does not delete subunit when membership check fails`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            runCatching { useCase(groupId, subunitId) }

            // Then
            coVerify(exactly = 0) { subunitRepository.deleteSubunit(any(), any()) }
        }

        @Test
        fun `calls requireMembership before deleting`() = runTest {
            // When
            useCase(groupId, subunitId)

            // Then
            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    // ── Invocation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invocation")
    inner class Invocation {

        @Test
        fun `delegates to repository deleteSubunit`() = runTest {
            // When
            useCase(groupId, subunitId)

            // Then
            coVerify(exactly = 1) { subunitRepository.deleteSubunit(groupId, subunitId) }
        }

        @Test
        fun `passes correct groupId and subunitId to repository`() = runTest {
            // Given
            val specificGroupId = "specific-group-789"
            val specificSubunitId = "specific-subunit-012"

            // When
            useCase(specificGroupId, specificSubunitId)

            // Then
            coVerify { subunitRepository.deleteSubunit(specificGroupId, specificSubunitId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            coEvery {
                subunitRepository.deleteSubunit(groupId, subunitId)
            } throws RuntimeException("DB error")

            // When / Then
            val exception = assertThrows<RuntimeException> {
                useCase(groupId, subunitId)
            }
            assertTrue(exception.message == "DB error")
        }
    }
}
