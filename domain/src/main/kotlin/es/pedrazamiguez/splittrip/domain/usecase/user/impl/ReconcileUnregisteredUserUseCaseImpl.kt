package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase

class ReconcileUnregisteredUserUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ReconcileUnregisteredUserUseCase {

    override suspend operator fun invoke(email: String, activeUserId: String): Result<Unit> = runCatching {
        val pendingUserId = User.generatePendingUserId(email)
        groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId)
        userRepository.deletePendingUser(pendingUserId).getOrThrow()
    }
}
