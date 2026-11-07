package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsByIdsUseCase

class GetCashWithdrawalsByIdsUseCaseImpl(
    private val cashWithdrawalRepository: CashWithdrawalRepository
) : GetCashWithdrawalsByIdsUseCase {
    override suspend operator fun invoke(withdrawalIds: List<String>): List<CashWithdrawal> {
        if (withdrawalIds.isEmpty()) return emptyList()
        return cashWithdrawalRepository.getWithdrawalsByIds(withdrawalIds)
    }
}
