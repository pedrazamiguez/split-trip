package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface GetGroupSpendingInsightsFlowUseCase : UseCase {
    operator fun invoke(
        groupId: String,
        periodFlow: Flow<SpendingInsightsPeriod>,
        filterCriteriaFlow: Flow<ExpenseFilterCriteria> = flowOf(ExpenseFilterCriteria()),
        todayProvider: () -> LocalDate = { LocalDate.now() }
    ): Flow<GroupSpendingInsights>
}
