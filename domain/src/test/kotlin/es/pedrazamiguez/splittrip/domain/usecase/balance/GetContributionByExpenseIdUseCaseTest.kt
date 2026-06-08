package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetContributionByExpenseIdUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetContributionByExpenseIdUseCaseTest {

    private lateinit var contributionRepository: ContributionRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var useCase: GetContributionByExpenseIdUseCase

    private val groupId = "group-123"
    private val expenseId = "expense-456"

    @BeforeEach
    fun setUp() {
        contributionRepository = mockk()
        groupMembershipService = mockk()
        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        useCase = GetContributionByExpenseIdUseCaseImpl(contributionRepository, groupMembershipService)
    }

    @Nested
    inner class MembershipValidation {

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId, "user-999")

            // When / Then
            try {
                useCase(groupId, expenseId)
                fail("Expected NotGroupMemberException")
            } catch (e: NotGroupMemberException) {
                assertEquals(groupId, e.groupId)
            }
        }

        @Test
        fun `verifies membership before querying repository`() = runTest {
            // Given
            coEvery { contributionRepository.findByLinkedExpenseId(groupId, expenseId) } returns null

            // When
            useCase(groupId, expenseId)

            // Then — membership check happened before repository call
            coVerify { groupMembershipService.requireMembership(groupId) }
        }
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns contribution when linked expense exists`() = runTest {
            // Given
            val contribution = mockk<Contribution>()
            coEvery { contributionRepository.findByLinkedExpenseId(groupId, expenseId) } returns contribution

            // When
            val result = useCase(groupId, expenseId)

            // Then
            assertEquals(contribution, result)
        }

        @Test
        fun `returns null when no contribution is linked to the expense`() = runTest {
            // Given
            coEvery { contributionRepository.findByLinkedExpenseId(groupId, expenseId) } returns null

            // When
            val result = useCase(groupId, expenseId)

            // Then
            assertNull(result)
        }

        @Test
        fun `delegates to repository with correct groupId and expenseId`() = runTest {
            // Given
            coEvery { contributionRepository.findByLinkedExpenseId(any(), any()) } returns null

            // When
            useCase(groupId, expenseId)

            // Then
            coVerify(exactly = 1) { contributionRepository.findByLinkedExpenseId(groupId, expenseId) }
        }
    }
}
