package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetActiveAiEngineUseCase

class SetActiveAiEngineUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : SetActiveAiEngineUseCase {

    override suspend operator fun invoke(engineType: AiEngineType) {
        preferenceRepository.setActiveAiEngine(engineType)
    }
}
