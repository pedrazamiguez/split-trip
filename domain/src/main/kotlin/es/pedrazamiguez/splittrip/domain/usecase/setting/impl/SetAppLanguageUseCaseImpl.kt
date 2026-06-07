package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase

class SetAppLanguageUseCaseImpl(private val preferenceRepository: UserPreferenceRepository) : SetAppLanguageUseCase {

    override suspend operator fun invoke(languageCode: String) {
        preferenceRepository.setAppLanguage(languageCode)
        preferenceRepository.setShouldShowLanguagePill(true)
    }
}
