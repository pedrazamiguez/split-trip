package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase

class GetSettlementSuggestionsUseCaseImpl(
    private val debtSimplificationService: DebtSimplificationService
) : GetSettlementSuggestionsUseCase {
    override operator fun invoke(memberBalances: List<MemberBalance>): List<Settlement> =
        debtSimplificationService.simplify(memberBalances)
}
