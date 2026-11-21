package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.SpendingInsightsService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import java.time.LocalDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
class GetGroupSpendingInsightsFlowUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val subunitRepository: SubunitRepository,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val spendingInsightsService: SpendingInsightsService,
    private val authenticationService: AuthenticationService,
    private val appConfigService: AppConfigService
) : GetGroupSpendingInsightsFlowUseCase {

    private data class GroupInsightsSnapshot(
        val group: Group?,
        val expenses: List<Expense>,
        val subunits: List<Subunit>
    )

    override operator fun invoke(
        groupId: String,
        periodFlow: Flow<SpendingInsightsPeriod>,
        todayProvider: () -> LocalDate
    ): Flow<GroupSpendingInsights> {
        val debouncedSnapshotFlow = combine(
            groupRepository.getGroupByIdFlow(groupId),
            expenseRepository.getGroupExpensesFlow(groupId),
            subunitRepository.getGroupSubunitsFlow(groupId)
        ) { group, expenses, subunits ->
            GroupInsightsSnapshot(
                group = group,
                expenses = expenses,
                subunits = subunits
            )
        }.debounce { appConfigService.balanceComputationDebounceMs.value }

        return combine(debouncedSnapshotFlow, periodFlow) { snapshot, period ->
            val currency = snapshot.group?.currency ?: appConfigService.defaultCurrencyCode.value
            val groupMemberIds = snapshot.group?.members.orEmpty()
            val allUserIds = buildSet {
                addAll(groupMemberIds)
                snapshot.subunits.forEach { subunit -> addAll(subunit.memberIds) }
                snapshot.expenses.forEach { expense ->
                    expense.splits.forEach { split -> add(split.userId) }
                }
            }.toList()

            val memberProfiles = getMemberProfilesUseCase(allUserIds)
            val currentUserId = authenticationService.currentUserId()

            spendingInsightsService.calculate(
                groupId = groupId,
                currency = currency,
                expenses = snapshot.expenses,
                subunits = snapshot.subunits,
                groupMemberIds = groupMemberIds,
                period = period,
                memberProfiles = memberProfiles,
                currentUserId = currentUserId,
                today = todayProvider()
            )
        }
    }
}
