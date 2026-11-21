package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.time.LocalDate

interface SpendingInsightsService {

    @Suppress("LongParameterList")
    fun calculate(
        groupId: String,
        currency: String,
        expenses: List<Expense>,
        subunits: List<Subunit>,
        groupMemberIds: List<String>,
        period: SpendingInsightsPeriod,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        today: LocalDate = LocalDate.now(),
        withdrawals: List<CashWithdrawal> = emptyList(),
        isFiltered: Boolean = false
    ): GroupSpendingInsights
}
