package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppThemeUseCase

class SetAppThemeUseCaseImpl(private val preferenceRepository: UserPreferenceRepository) : SetAppThemeUseCase {

    override suspend operator fun invoke(themeCode: String) {
        preferenceRepository.setAppTheme(themeCode)
    }
}
