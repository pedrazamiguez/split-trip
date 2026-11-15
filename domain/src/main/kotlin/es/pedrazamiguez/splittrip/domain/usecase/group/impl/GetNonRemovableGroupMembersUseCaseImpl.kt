package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.strategy.StandardContributionAttributionStrategy
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.MemberBalanceCalculationInputs
import es.pedrazamiguez.splittrip.domain.usecase.group.GetNonRemovableGroupMembersUseCase
import kotlinx.coroutines.flow.first

class GetNonRemovableGroupMembersUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val contributionRepository: ContributionRepository,
    private val cashWithdrawalRepository: CashWithdrawalRepository,
    private val subunitRepository: SubunitRepository,
    private val cashTransferRepository: CashTransferRepository,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase,
    private val settlementRepository: SettlementRepository
) : GetNonRemovableGroupMembersUseCase {

    override suspend operator fun invoke(groupId: String): Map<String, CannotRemoveMemberException.Reason> {
        val group = groupRepository.getGroupById(groupId) ?: return emptyMap()
        if (group.members.isEmpty()) return emptyMap()

        val expenses = expenseRepository.getGroupExpensesFlow(groupId).first()
        val contributions = contributionRepository.getGroupContributionsFlow(groupId).first()
        val withdrawals = cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId).first()
        val subunits = subunitRepository.getGroupSubunits(groupId)
        val cashTransfers = cashTransferRepository.observeGroupCashTransfers(groupId).first()
        val settlements = settlementRepository.getGroupSettlements(groupId)

        val balances = getMemberBalancesFlowUseCase.computeMemberBalances(
            MemberBalanceCalculationInputs(
                contributions = contributions,
                withdrawals = withdrawals,
                expenses = expenses,
                cashTransfers = cashTransfers,
                subunits = subunits,
                groupMemberIds = group.members,
                groupCurrency = group.currency,
                settlements = settlements,
                attributionStrategy = StandardContributionAttributionStrategy
            )
        )

        val isSingleMember = group.members.size <= 1
        return buildMap {
            for (memberId in group.members) {
                val reason = when {
                    memberId == group.createdBy -> CannotRemoveMemberException.Reason.IS_CREATOR
                    isSingleMember -> CannotRemoveMemberException.Reason.LAST_MEMBER
                    else -> {
                        val memberBalance = balances.find { it.userId == memberId }
                        when {
                            memberBalance == null -> CannotRemoveMemberException.Reason.USER_NOT_IN_BALANCES
                            memberBalance.totalBalance != 0L -> CannotRemoveMemberException.Reason.NON_ZERO_BALANCE
                            else -> null
                        }
                    }
                }
                if (reason != null) {
                    put(memberId, reason)
                }
            }
        }
    }
}
