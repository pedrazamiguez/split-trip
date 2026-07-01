package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase
import kotlinx.coroutines.flow.first

class RemoveGroupMemberUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val subunitRepository: SubunitRepository,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
) : RemoveGroupMemberUseCase {

    override suspend operator fun invoke(groupId: String, userId: String): Result<Unit> = runCatching {
        val group = groupRepository.getGroupById(groupId)
            ?: throw IllegalArgumentException("Group not found: $groupId")

        if (group.status == GroupStatus.ARCHIVED) throw GroupArchivedException(groupId)
        if (userId !in group.members) throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.NOT_A_MEMBER)
        if (group.createdBy == userId) throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.IS_CREATOR)
        if (group.members.size <= 1) throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.LAST_MEMBER)

        val expenses = expenseRepository.getGroupExpensesFlow(groupId).first()
        val contributions = contributionRepository.getGroupContributionsFlow(groupId).first()
        val withdrawals = cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId).first()
        val subunits = subunitRepository.getGroupSubunits(groupId)

        val balances = getMemberBalancesFlowUseCase.computeMemberBalances(
            contributions = contributions,
            withdrawals = withdrawals,
            expenses = expenses,
            subunits = subunits,
            groupMemberIds = group.members,
            groupCurrency = group.currency
        )

        val memberBalance = balances.find { it.userId == userId }
            ?: throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.USER_NOT_IN_BALANCES)

        if (memberBalance.totalBalance != 0L) {
            throw CannotRemoveMemberException(CannotRemoveMemberException.Reason.NON_ZERO_BALANCE)
        }

        groupRepository.removeMember(groupId, userId)
    }
}
