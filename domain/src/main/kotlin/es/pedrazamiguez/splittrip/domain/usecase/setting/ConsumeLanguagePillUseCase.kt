package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository

class ConsumeLanguagePillUseCase(private val preferenceRepository: UserPreferenceRepository) {

    suspend operator fun invoke() {
        preferenceRepository.setShouldShowLanguagePill(false)
    }
}
