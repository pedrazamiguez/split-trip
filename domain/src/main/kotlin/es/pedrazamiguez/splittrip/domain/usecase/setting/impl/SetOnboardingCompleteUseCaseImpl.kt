package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetOnboardingCompleteUseCase

class SetOnboardingCompleteUseCaseImpl(
    private val preferenceRepository: OnboardingPreferenceRepository
) : SetOnboardingCompleteUseCase {

    override suspend operator fun invoke() {
        preferenceRepository.setOnboardingComplete()
    }
}
