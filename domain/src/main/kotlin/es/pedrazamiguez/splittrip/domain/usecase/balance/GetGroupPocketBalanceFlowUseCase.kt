package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupPocketBalanceFlowUseCase : UseCase {
    operator fun invoke(groupId: String, currency: String): Flow<GroupPocketBalance>

    fun calculate(
        contributions: List<Contribution>,
        expenses: List<Expense>,
        withdrawals: List<CashWithdrawal>,
        currency: String
    ): GroupPocketBalance
}
