package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository

class SetAppLanguageUseCase(private val preferenceRepository: UserPreferenceRepository) {

    suspend operator fun invoke(languageCode: String) {
        preferenceRepository.setAppLanguage(languageCode)
        preferenceRepository.setShouldShowLanguagePill(true)
    }
}
