package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import kotlinx.coroutines.flow.Flow

class GetSelectedGroupNameUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : GetSelectedGroupNameUseCase {

    override operator fun invoke(): Flow<String?> = preferenceRepository.getSelectedGroupName()
}
