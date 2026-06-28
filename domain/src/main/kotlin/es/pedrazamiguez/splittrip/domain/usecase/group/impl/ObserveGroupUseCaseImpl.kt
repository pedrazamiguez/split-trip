package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import kotlinx.coroutines.flow.Flow

class ObserveGroupUseCaseImpl(
    private val groupRepository: GroupRepository
) : ObserveGroupUseCase {

    override operator fun invoke(groupId: String): Flow<Group?> {
        return groupRepository.getGroupByIdFlow(groupId)
    }
}
