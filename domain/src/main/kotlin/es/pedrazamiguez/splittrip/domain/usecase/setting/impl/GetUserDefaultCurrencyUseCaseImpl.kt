package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import kotlinx.coroutines.flow.Flow

class GetUserDefaultCurrencyUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : GetUserDefaultCurrencyUseCase {

    override operator fun invoke(): Flow<String> = preferenceRepository.getUserDefaultCurrency()
}
