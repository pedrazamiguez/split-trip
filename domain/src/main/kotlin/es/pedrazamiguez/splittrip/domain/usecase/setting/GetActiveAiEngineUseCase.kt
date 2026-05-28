package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.Flow

class GetActiveAiEngineUseCase(private val preferenceRepository: UserPreferenceRepository) {

    operator fun invoke(): Flow<AiEngineType> = preferenceRepository.getActiveAiEngine()
}
