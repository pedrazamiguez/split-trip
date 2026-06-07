package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetActiveAiEngineUseCase : UseCase {
    suspend operator fun invoke(engineType: AiEngineType)
}
