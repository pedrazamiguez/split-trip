package es.pedrazamiguez.splittrip.domain.usecase.setting.impl

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase

class SetSelectedGroupUseCaseImpl(
    private val preferenceRepository: GroupPreferenceRepository
) : SetSelectedGroupUseCase {

    override suspend operator fun invoke(groupId: String?, groupName: String?, currency: String?) {
        preferenceRepository.setSelectedGroup(groupId, groupName, currency)
    }
}
