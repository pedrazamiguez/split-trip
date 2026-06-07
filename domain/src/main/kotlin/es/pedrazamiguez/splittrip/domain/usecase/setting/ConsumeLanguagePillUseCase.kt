package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface ConsumeLanguagePillUseCase : UseCase {
    suspend operator fun invoke()
}
