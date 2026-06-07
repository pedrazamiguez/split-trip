package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import kotlinx.coroutines.flow.Flow

class GetShouldShowLanguagePillUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : GetShouldShowLanguagePillUseCase {

    override operator fun invoke(): Flow<Boolean> = preferenceRepository.getShouldShowLanguagePill()
}
