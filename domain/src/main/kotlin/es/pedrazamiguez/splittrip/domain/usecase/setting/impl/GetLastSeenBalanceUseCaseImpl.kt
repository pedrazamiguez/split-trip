package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import kotlinx.coroutines.flow.Flow

class GetLastSeenBalanceUseCaseImpl(
    private val balancePreferenceRepository: BalancePreferenceRepository
) : GetLastSeenBalanceUseCase {

    override operator fun invoke(groupId: String): Flow<String?> = balancePreferenceRepository.getLastSeenBalance(
        groupId
    )
}
