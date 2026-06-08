package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase

class ConsumeLanguagePillUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : ConsumeLanguagePillUseCase {

    override suspend operator fun invoke() {
        preferenceRepository.setShouldShowLanguagePill(false)
    }
}
