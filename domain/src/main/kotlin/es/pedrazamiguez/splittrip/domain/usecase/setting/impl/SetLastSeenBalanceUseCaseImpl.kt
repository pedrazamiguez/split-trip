package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase

class SetLastSeenBalanceUseCaseImpl(
    private val balancePreferenceRepository: BalancePreferenceRepository
) : SetLastSeenBalanceUseCase {

    override suspend operator fun invoke(groupId: String, formattedBalance: String) {
        balancePreferenceRepository.setLastSeenBalance(groupId, formattedBalance)
    }
}
