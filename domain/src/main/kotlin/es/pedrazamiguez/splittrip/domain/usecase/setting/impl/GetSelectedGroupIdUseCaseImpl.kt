package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import kotlinx.coroutines.flow.Flow

class GetSelectedGroupIdUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetSelectedGroupIdUseCase {

    override operator fun invoke(): Flow<String?> = preferenceRepository.getSelectedGroupId()
}
