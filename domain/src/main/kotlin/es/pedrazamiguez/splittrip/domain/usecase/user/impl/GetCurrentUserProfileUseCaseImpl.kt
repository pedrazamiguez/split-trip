package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase

class GetCurrentUserProfileUseCaseImpl(private val userRepository: UserRepository) : GetCurrentUserProfileUseCase {

    override suspend operator fun invoke(): User? = userRepository.getCurrentUserProfile()
}
