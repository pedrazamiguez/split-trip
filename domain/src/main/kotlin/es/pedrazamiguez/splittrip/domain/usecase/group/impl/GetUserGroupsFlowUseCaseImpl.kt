package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetUserGroupsFlowUseCaseImpl(private val groupRepository: GroupRepository) : GetUserGroupsFlowUseCase {

    override operator fun invoke(): Flow<List<Group>> = groupRepository.getAllGroupsFlow()
}
