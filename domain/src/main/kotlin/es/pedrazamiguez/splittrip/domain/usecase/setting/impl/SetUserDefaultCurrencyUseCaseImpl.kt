package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase

class SetUserDefaultCurrencyUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : SetUserDefaultCurrencyUseCase {

    override suspend operator fun invoke(currencyCode: String) {
        preferenceRepository.setUserDefaultCurrency(currencyCode)
    }
}
