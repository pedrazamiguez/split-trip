package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetSettlementSuggestionsUseCase : UseCase {
    operator fun invoke(memberBalances: List<MemberBalance>): List<Settlement>
}
