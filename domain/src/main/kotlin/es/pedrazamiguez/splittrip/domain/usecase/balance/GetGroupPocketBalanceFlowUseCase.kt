package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupPocketBalanceFlowUseCase : UseCase {
    operator fun invoke(groupId: String, currency: String): Flow<GroupPocketBalance>
}
