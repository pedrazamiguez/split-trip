package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import kotlinx.coroutines.flow.Flow

class GetSelectedGroupCurrencyUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetSelectedGroupCurrencyUseCase {

    override operator fun invoke(): Flow<String?> = preferenceRepository.getSelectedGroupCurrency()
}
