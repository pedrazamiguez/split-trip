package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase

class SearchUsersByEmailUseCaseImpl(private val userRepository: UserRepository) : SearchUsersByEmailUseCase {

    override suspend operator fun invoke(email: String): Result<List<User>> = runCatching {
        userRepository.searchUsersByEmail(email)
    }
}
