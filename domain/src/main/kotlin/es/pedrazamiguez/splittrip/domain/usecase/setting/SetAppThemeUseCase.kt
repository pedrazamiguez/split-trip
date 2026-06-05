package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository

class SetAppThemeUseCase(private val preferenceRepository: UserPreferenceRepository) {

    suspend operator fun invoke(themeCode: String) {
        preferenceRepository.setAppTheme(themeCode)
    }
}
