package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.Flow

class GetShouldShowLanguagePillUseCase(private val preferenceRepository: UserPreferenceRepository) {

    operator fun invoke(): Flow<Boolean> = preferenceRepository.getShouldShowLanguagePill()
}
