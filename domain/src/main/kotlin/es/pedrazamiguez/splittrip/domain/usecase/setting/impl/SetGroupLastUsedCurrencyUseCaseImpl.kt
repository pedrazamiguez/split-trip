package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetGroupLastUsedCurrencyUseCase

class SetGroupLastUsedCurrencyUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : SetGroupLastUsedCurrencyUseCase {

    override suspend operator fun invoke(groupId: String, currencyCode: String) {
        preferenceRepository.setGroupLastUsedCurrency(groupId, currencyCode)
    }
}
