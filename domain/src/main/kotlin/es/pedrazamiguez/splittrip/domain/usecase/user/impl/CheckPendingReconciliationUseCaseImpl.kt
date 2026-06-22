package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase

class CheckPendingReconciliationUseCaseImpl(
    private val userRepository: UserRepository
) : CheckPendingReconciliationUseCase {
    override suspend operator fun invoke(email: String): Result<Boolean> = runCatching {
        val pendingUserId = User.generatePendingUserId(email)
        val users = userRepository.getUsersByIds(listOf(pendingUserId))
        users.containsKey(pendingUserId)
    }
}
