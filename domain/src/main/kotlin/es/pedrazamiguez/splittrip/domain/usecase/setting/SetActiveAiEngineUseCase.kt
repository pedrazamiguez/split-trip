package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository

class SetActiveAiEngineUseCase(private val preferenceRepository: UserPreferenceRepository) {

    suspend operator fun invoke(engineType: AiEngineType) {
        preferenceRepository.setActiveAiEngine(engineType)
    }
}
