package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetActiveAiEngineUseCase
import kotlinx.coroutines.flow.Flow

class GetActiveAiEngineUseCaseImpl(
    private val preferenceRepository: UserPreferenceRepository
) : GetActiveAiEngineUseCase {

    override operator fun invoke(): Flow<AiEngineType> = preferenceRepository.getActiveAiEngine()
}
