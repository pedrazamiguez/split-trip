package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetCashWithdrawalsByIdsUseCase : UseCase {
    suspend operator fun invoke(withdrawalIds: List<String>): List<CashWithdrawal>
}
