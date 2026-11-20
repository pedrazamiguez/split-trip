package es.pedrazamiguez.splittrip.data.datasource

import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupDashboardReadModel
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

class GroupDashboardDataSourceImpl(
    private val groupRepository: GroupRepository,
    private val contributionRepository: ContributionRepository,
    private val withdrawalRepository: CashWithdrawalRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val subunitRepository: SubunitRepository,
    private val cashTransferRepository: CashTransferRepository
) : GroupDashboardDataSource {

    private data class PrimaryDashboardStreams(
        val group: Group,
        val contributions: List<Contribution>,
        val withdrawals: List<CashWithdrawal>
    )

    private data class SecondaryDashboardStreams(
        val subunits: List<Subunit>,
        val expenses: List<Expense>,
        val settlements: List<SettlementRecord>,
        val cashTransfers: List<CashTransfer>
    )

    override fun getDashboardSnapshotFlow(groupId: String): Flow<GroupDashboardReadModel> {
        return combine(
            combine(
                groupRepository.getGroupByIdFlow(groupId).filterNotNull(),
                contributionRepository.getGroupContributionsFlow(groupId),
                withdrawalRepository.getGroupWithdrawalsFlow(groupId)
            ) { group, contributions, withdrawals ->
                PrimaryDashboardStreams(
                    group = group,
                    contributions = contributions,
                    withdrawals = withdrawals
                )
            },
            combine(
                subunitRepository.getGroupSubunitsFlow(groupId),
                expenseRepository.getGroupExpensesFlow(groupId),
                settlementRepository.getGroupSettlementsFlow(groupId),
                cashTransferRepository.observeGroupCashTransfers(groupId)
            ) { subunits, expenses, settlements, cashTransfers ->
                SecondaryDashboardStreams(
                    subunits = subunits,
                    expenses = expenses,
                    settlements = settlements,
                    cashTransfers = cashTransfers
                )
            }
        ) { primary, secondary ->
            GroupDashboardReadModel(
                group = primary.group,
                contributions = primary.contributions,
                withdrawals = primary.withdrawals,
                subunits = secondary.subunits,
                expenses = secondary.expenses,
                settlements = secondary.settlements,
                cashTransfers = secondary.cashTransfers
            )
        }
    }
}
