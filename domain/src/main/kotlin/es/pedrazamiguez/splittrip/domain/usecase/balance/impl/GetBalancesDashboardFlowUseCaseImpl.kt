package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.model.BalancesDashboardDomainModel
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesDashboardFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.MemberBalanceCalculationInputs
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class)
class GetBalancesDashboardFlowUseCaseImpl(
    private val groupDashboardDataSource: GroupDashboardDataSource,
    private val getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase,
    private val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase,
    private val getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val appConfigService: AppConfigService
) : GetBalancesDashboardFlowUseCase {
    override fun invoke(
        groupId: String,
        currency: String,
        groupMemberIds: List<String>
    ): Flow<BalancesDashboardDomainModel> {
        return groupDashboardDataSource.getDashboardSnapshotFlow(groupId)
            .debounce { appConfigService.balanceComputationDebounceMs.value }
            .map { snapshot ->
                val balance = getGroupPocketBalanceFlowUseCase.calculate(
                    contributions = snapshot.contributions,
                    expenses = snapshot.expenses,
                    withdrawals = snapshot.withdrawals,
                    currency = currency
                )
                val memberBalances = getMemberBalancesFlowUseCase.computeMemberBalances(
                    MemberBalanceCalculationInputs(
                        contributions = snapshot.contributions,
                        withdrawals = snapshot.withdrawals,
                        expenses = snapshot.expenses,
                        cashTransfers = snapshot.cashTransfers,
                        subunits = snapshot.subunits,
                        groupMemberIds = groupMemberIds,
                        groupCurrency = currency,
                        settlements = snapshot.settlements
                    )
                )

                val allUserIds = buildSet {
                    addAll(groupMemberIds)
                    snapshot.contributions.forEach { add(it.userId) }
                    snapshot.withdrawals.forEach { add(it.withdrawnBy) }
                    memberBalances.forEach { add(it.userId) }
                }.toList()
                val memberProfiles = getMemberProfilesUseCase(allUserIds)

                val settlementSuggestions = getSettlementSuggestionsUseCase(memberBalances)

                BalancesDashboardDomainModel(
                    balance = balance,
                    contributions = snapshot.contributions,
                    withdrawals = snapshot.withdrawals,
                    subunits = snapshot.subunits,
                    expenses = snapshot.expenses,
                    settlements = snapshot.settlements,
                    memberBalances = memberBalances,
                    settlementSuggestions = settlementSuggestions,
                    memberProfiles = memberProfiles
                )
            }
    }
}
