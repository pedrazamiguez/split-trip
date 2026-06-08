package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.IsOnboardingCompleteUseCase
import kotlinx.coroutines.flow.Flow

class IsOnboardingCompleteUseCaseImpl(
    private val preferenceRepository: OnboardingPreferenceRepository
) : IsOnboardingCompleteUseCase {

    override operator fun invoke(): Flow<Boolean> = preferenceRepository.isOnboardingComplete()
}
