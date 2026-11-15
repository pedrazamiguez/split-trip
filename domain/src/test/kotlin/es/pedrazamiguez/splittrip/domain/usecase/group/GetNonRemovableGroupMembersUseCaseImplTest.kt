package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetNonRemovableGroupMembersUseCaseImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetNonRemovableGroupMembersUseCaseImplTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var cashTransferRepository: CashTransferRepository
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
    private lateinit var settlementRepository: SettlementRepository
    private lateinit var useCase: GetNonRemovableGroupMembersUseCase

    private val groupId = "group-123"
    private val creatorId = "user-creator"
    private val memberId = "user-member"
    private val otherMember = "user-other"

    private val testGroup = Group(
        id = groupId,
        name = "Test Group",
        members = listOf(creatorId, memberId, otherMember),
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
        cashTransferRepository = mockk()
        getMemberBalancesFlowUseCase = mockk()
        settlementRepository = mockk()

        coEvery { groupRepository.getGroupById(groupId) } returns testGroup
        every { expenseRepository.getGroupExpensesFlow(any()) } returns flowOf(emptyList())
        every { contributionRepository.getGroupContributionsFlow(any()) } returns flowOf(emptyList())
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(any()) } returns flowOf(emptyList())
        coEvery { subunitRepository.getGroupSubunits(any()) } returns emptyList()
        every { cashTransferRepository.observeGroupCashTransfers(any()) } returns flowOf(emptyList())
        coEvery { settlementRepository.getGroupSettlements(any()) } returns emptyList()

        useCase = GetNonRemovableGroupMembersUseCaseImpl(
            groupRepository = groupRepository,
            expenseRepository = expenseRepository,
            contributionRepository = contributionRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            subunitRepository = subunitRepository,
            cashTransferRepository = cashTransferRepository,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            settlementRepository = settlementRepository
        )
    }

    @Test
    fun `returns empty map when group is not found`() = runTest {
        coEvery { groupRepository.getGroupById(groupId) } returns null

        val result = useCase(groupId)

        assertEquals(emptyMap<String, CannotRemoveMemberException.Reason>(), result)
    }

    @Test
    fun `marks group creator as IS_CREATOR`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = creatorId),
            MemberBalance(userId = memberId),
            MemberBalance(userId = otherMember)
        )

        val result = useCase(groupId)

        assertEquals(CannotRemoveMemberException.Reason.IS_CREATOR, result[creatorId])
    }

    @Test
    fun `marks member with non-zero balance as NON_ZERO_BALANCE`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = creatorId),
            MemberBalance(userId = memberId, pocketBalance = 500L),
            MemberBalance(userId = otherMember)
        )

        val result = useCase(groupId)

        assertEquals(CannotRemoveMemberException.Reason.NON_ZERO_BALANCE, result[memberId])
    }

    @Test
    fun `marks member with non-zero cashInHand as NON_ZERO_BALANCE`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = creatorId),
            MemberBalance(userId = memberId, pocketBalance = 0L, cashInHand = 1000L),
            MemberBalance(userId = otherMember)
        )

        val result = useCase(groupId)

        assertEquals(CannotRemoveMemberException.Reason.NON_ZERO_BALANCE, result[memberId])
    }

    @Test
    fun `omits non-creator member with zero balance`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = creatorId),
            MemberBalance(userId = memberId),
            MemberBalance(userId = otherMember)
        )

        val result = useCase(groupId)

        assertFalse(memberId in result)
        assertFalse(otherMember in result)
    }

    @Test
    fun `marks sole non-creator member as LAST_MEMBER`() = runTest {
        coEvery { groupRepository.getGroupById(groupId) } returns testGroup.copy(
            members = listOf(otherMember),
            createdBy = creatorId
        )
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns listOf(
            MemberBalance(userId = otherMember)
        )

        val result = useCase(groupId)

        assertEquals(CannotRemoveMemberException.Reason.LAST_MEMBER, result[otherMember])
    }

    @Test
    fun `marks member missing from balances as USER_NOT_IN_BALANCES`() = runTest {
        coEvery { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()

        val result = useCase(groupId)

        assertEquals(CannotRemoveMemberException.Reason.USER_NOT_IN_BALANCES, result[otherMember])
    }
}
