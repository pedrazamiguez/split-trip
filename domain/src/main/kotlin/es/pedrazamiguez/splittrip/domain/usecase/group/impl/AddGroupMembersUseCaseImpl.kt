package es.pedrazamiguez.splittrip.domain.usecase.group.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.AddGroupMembersUseCase

class AddGroupMembersUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : AddGroupMembersUseCase {

    override suspend operator fun invoke(groupId: String, newMembers: List<User>): Result<Unit> = runCatching {
        newMembers.filter { it.isPending }.forEach { pendingUser ->
            userRepository.saveUser(pendingUser).getOrThrow()
        }
        groupRepository.addMembers(groupId, newMembers.map { it.userId })
    }
}
