package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetCashWithdrawalsFlowUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository
) : GetCashWithdrawalsFlowUseCase {

    override operator fun invoke(groupId: String): Flow<List<CashWithdrawal>> =
        cashWithdrawalRepository.getGroupWithdrawalsFlow(groupId)
}
