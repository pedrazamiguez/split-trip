package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetOnboardingCompleteUseCase : UseCase {
    suspend operator fun invoke()
}
