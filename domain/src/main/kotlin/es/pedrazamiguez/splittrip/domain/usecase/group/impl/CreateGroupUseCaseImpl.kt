package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase

class CreateGroupUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : CreateGroupUseCase {

    override suspend operator fun invoke(group: Group, members: List<User>): Result<String> = runCatching {
        members.filter { it.isPending }.forEach { pendingUser ->
            userRepository.saveUser(pendingUser).getOrThrow()
        }
        groupRepository.createGroup(group)
    }
}
