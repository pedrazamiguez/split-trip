package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserProfileUseCaseImpl(
    private val userRepository: UserRepository
) : ObserveCurrentUserProfileUseCase {

    override operator fun invoke(): Flow<User?> {
        return userRepository.observeCurrentUserProfile()
    }
}
