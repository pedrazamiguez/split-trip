package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ObserveSelectedGroupUseCaseImpl(
    private val groupPreferenceRepository: GroupPreferenceRepository,
    private val groupRepository: GroupRepository
) : ObserveSelectedGroupUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override operator fun invoke(): Flow<Group?> {
        return groupPreferenceRepository.getSelectedGroupId().flatMapLatest { groupId ->
            if (groupId == null) {
                flowOf(null)
            } else {
                groupRepository.getGroupByIdFlow(groupId)
            }
        }
    }
}
