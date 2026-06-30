package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotLeaveGroupException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.LeaveGroupUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LeaveGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
    private lateinit var useCase: LeaveGroupUseCase

    private val groupId = "group-123"
    private val currentUserId = "user-1"
    private val anotherUserId = "user-2"

    private val sampleGroup = Group(
        id = groupId,
        name = "Trip to Paris",
        currency = "EUR",
        members = listOf(currentUserId, anotherUserId),
        status = GroupStatus.ACTIVE,
        createdBy = anotherUserId,
        createdAt = LocalDateTime.now(),
        lastUpdatedAt = LocalDateTime.now()
    )

    private val zeroBalance = MemberBalance(
        userId = currentUserId
    )

    private val nonZeroBalance = MemberBalance(
        userId = currentUserId,
        pocketBalance = 1000L
    )

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        authenticationService = mockk()
        expenseRepository = mockk()
        contributionRepository = mockk()
        cashWithdrawalRepository = mockk()
        subunitRepository = mockk()
        getMemberBalancesFlowUseCase = mockk()

        useCase = LeaveGroupUseCaseImpl(
            groupRepository = groupRepository,
            authenticationService = authenticationService,
            expenseRepository = expenseRepository,
            contributionRepository = contributionRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            subunitRepository = subunitRepository,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase
        )

        every { authenticationService.requireUserId() } returns currentUserId
        coEvery { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        coEvery { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        coEvery { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
        coEvery { groupRepository.updateGroup(any()) } just Runs
    }

    @Nested
    inner class Invocation {

        @Test
        fun `leaves group successfully when balance is zero`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns sampleGroup
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    expenses = emptyList(),
                    subunits = emptyList(),
                    groupMemberIds = sampleGroup.members,
                    groupCurrency = sampleGroup.currency
                )
            } returns listOf(zeroBalance, MemberBalance(userId = anotherUserId))

            val result = useCase(groupId)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                groupRepository.updateGroup(
                    match { updatedGroup ->
                        updatedGroup.members == listOf(anotherUserId)
                    }
                )
            }
        }

        @Test
        fun `throws when group is not found`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns null

            val result = useCase(groupId)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun `throws when group is archived`() = runTest {
            val archivedGroup = sampleGroup.copy(status = GroupStatus.ARCHIVED)
            coEvery { groupRepository.getGroupById(groupId) } returns archivedGroup

            val result = useCase(groupId)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is GroupArchivedException)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }

        @Test
        fun `throws when user is not a member`() = runTest {
            val groupWithoutUser = sampleGroup.copy(members = listOf(anotherUserId))
            every { authenticationService.requireUserId() } returns currentUserId
            coEvery { groupRepository.getGroupById(groupId) } returns groupWithoutUser

            val result = useCase(groupId)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is CannotLeaveGroupException)
            assertEquals("Cannot leave group: not_a_member", exception?.message)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }

        @Test
        fun `throws when user is the creator`() = runTest {
            val groupAsCreator = sampleGroup.copy(createdBy = currentUserId)
            coEvery { groupRepository.getGroupById(groupId) } returns groupAsCreator

            val result = useCase(groupId)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is CannotLeaveGroupException)
            assertEquals("Cannot leave group: is_creator", exception?.message)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }

        @Test
        fun `throws when user has non-zero balance`() = runTest {
            coEvery { groupRepository.getGroupById(groupId) } returns sampleGroup
            every {
                getMemberBalancesFlowUseCase.computeMemberBalances(
                    contributions = emptyList(),
                    withdrawals = emptyList(),
                    expenses = emptyList(),
                    subunits = emptyList(),
                    groupMemberIds = sampleGroup.members,
                    groupCurrency = sampleGroup.currency
                )
            } returns listOf(nonZeroBalance, MemberBalance(userId = anotherUserId))

            val result = useCase(groupId)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is CannotLeaveGroupException)
            assertEquals("Cannot leave group: non_zero_balance", exception?.message)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }
    }
}
