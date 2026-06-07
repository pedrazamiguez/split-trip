package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SetSelectedGroupUseCase : UseCase {
    suspend operator fun invoke(groupId: String?, groupName: String?, currency: String? = null)
}
