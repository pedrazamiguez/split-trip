package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import kotlinx.coroutines.flow.first

class ReconcileUnregisteredUserUseCaseImpl(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository
) : ReconcileUnregisteredUserUseCase {

    override suspend operator fun invoke(email: String, activeUserId: String): Result<Unit> = runCatching {
        if (userPreferenceRepository.getIsReconciled().first()) {
            return@runCatching
        }
        val pendingUserId = User.generatePendingUserId(email)
        groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId)
        userRepository.deletePendingUser(pendingUserId).getOrThrow()
        userPreferenceRepository.setIsReconciled(true)
    }
}
