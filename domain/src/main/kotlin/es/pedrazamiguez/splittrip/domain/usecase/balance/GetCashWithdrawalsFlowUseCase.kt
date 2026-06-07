package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetCashWithdrawalsFlowUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<List<CashWithdrawal>>
}
