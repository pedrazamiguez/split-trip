package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteCashWithdrawalUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeleteCashWithdrawalUseCaseTest {

    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var groupRepository: GroupRepository
    private lateinit var useCase: DeleteCashWithdrawalUseCase

    @BeforeEach
    fun setUp() {
        cashWithdrawalRepository = mockk()
        groupMembershipService = mockk()
        groupRepository = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        coEvery { groupRepository.getGroupById(any()) } returns mockk {
            every { status } returns GroupStatus.ACTIVE
        }

        useCase = DeleteCashWithdrawalUseCaseImpl(
            cashWithdrawalRepository = cashWithdrawalRepository,
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
            val withdrawalId = "withdrawal-456"
            coEvery { groupRepository.getGroupById(groupId) } returns mockk {
                every { status } returns GroupStatus.ARCHIVED
            }

            // When / Then
            try {
                useCase(groupId, withdrawalId)
                fail("Expected GroupArchivedException to be thrown")
            } catch (e: GroupArchivedException) {
                // Expected
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
            val withdrawalId = "withdrawal-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When / Then
            try {
                useCase(groupId, withdrawalId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertTrue(e.groupId == groupId)
            }
        }

        @Test
        fun `does not delete withdrawal when membership check fails`() = runTest {
            // Given
            val groupId = "group-123"
            val withdrawalId = "withdrawal-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            runCatching { useCase(groupId, withdrawalId) }

            // Then
            coVerify(exactly = 0) { cashWithdrawalRepository.deleteWithdrawal(any(), any()) }
        }

        @Test
        fun `calls requireMembership before deleting`() = runTest {
            // Given
            val groupId = "group-123"
            val withdrawalId = "withdrawal-456"
            coEvery { cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId) } just Runs

            // When
            useCase(groupId, withdrawalId)

            // Then
            coVerifyOrder {
                groupMembershipService.requireMembership(groupId)
                cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId)
            }
        }
    }

    // ── Happy path ─────────────────────────────────────────────────────────────

    @Nested
    inner class Invocation {

        @Test
        fun `delegates to repository deleteWithdrawal`() = runTest {
            // Given
            val groupId = "group-123"
            val withdrawalId = "withdrawal-456"
            coEvery { cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId) } just Runs

            // When
            useCase(groupId, withdrawalId)

            // Then
            coVerify(exactly = 1) { cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId) }
        }

        @Test
        fun `passes correct groupId and withdrawalId to repository`() = runTest {
            // Given
            val groupId = "specific-group-id-789"
            val withdrawalId = "specific-withdrawal-id-012"
            coEvery { cashWithdrawalRepository.deleteWithdrawal(any(), any()) } just Runs

            // When
            useCase(groupId, withdrawalId)

            // Then
            coVerify { cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            val groupId = "group-123"
            val withdrawalId = "withdrawal-456"
            val exception = RuntimeException("Delete failed")
            coEvery { cashWithdrawalRepository.deleteWithdrawal(groupId, withdrawalId) } throws exception

            // When / Then
            try {
                useCase(groupId, withdrawalId)
                fail("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertTrue(e.message == "Delete failed")
            }
        }
    }
}
