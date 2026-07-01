package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.RemoveGroupMemberUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RemoveGroupMemberUseCaseImplTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
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
        expenseRepository = mockk()
        contributionRepository = mockk()
        cashWithdrawalRepository = mockk()
        subunitRepository = mockk()
        getMemberBalancesFlowUseCase = mockk()

        useCase = RemoveGroupMemberUseCaseImpl(
            groupRepository = groupRepository,
            expenseRepository = expenseRepository,
            contributionRepository = contributionRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            subunitRepository = subunitRepository,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase
        )

        coEvery { groupRepository.getGroupById(groupId) } returns testGroup
        coEvery { groupRepository.removeMember(any(), any()) } returns Unit
        coEvery { expenseRepository.getGroupExpensesFlow(groupId) } returns flowOf(emptyList())
        coEvery { contributionRepository.getGroupContributionsFlow(groupId) } returns flowOf(emptyList())
        coEvery { cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(groupId) } returns emptyList()
    }

    @Test
    fun `removes member when balance is zero and validation passes`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any(), any(), any(), any(), any(), any()) } returns
            listOf(MemberBalance(userId = memberToRemove))

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
    fun `fails when member is the creator`() = runTest {
        val result = useCase(groupId, creatorId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotRemoveMemberException)
        assertTrue(result.exceptionOrNull()?.message?.contains("is_creator") == true)
    }

    @Test
    fun `fails when member is not in group`() = runTest {
        val result = useCase(groupId, "non-member")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotRemoveMemberException)
        assertTrue(result.exceptionOrNull()?.message?.contains("not_a_member") == true)
    }

    @Test
    fun `fails when group has only one member`() = runTest {
        coEvery { groupRepository.getGroupById(groupId) } returns
            testGroup.copy(members = listOf(memberToRemove), createdBy = creatorId)

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotRemoveMemberException)
        assertTrue(result.exceptionOrNull()?.message?.contains("last_member") == true)
    }

    @Test
    fun `fails when member has non-zero balance`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any(), any(), any(), any(), any(), any()) } returns
            listOf(MemberBalance(userId = memberToRemove, pocketBalance = 5000))

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotRemoveMemberException)
        assertTrue(result.exceptionOrNull()?.message?.contains("non_zero_balance") == true)
    }

    @Test
    fun `fails when member balance entry is not found`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any(), any(), any(), any(), any(), any()) } returns
            emptyList()

        val result = useCase(groupId, memberToRemove)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CannotRemoveMemberException)
        assertTrue(result.exceptionOrNull()?.message?.contains("user_not_in_balances") == true)
    }
}
