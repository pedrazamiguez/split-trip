package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase

class GetMemberProfilesUseCaseImpl(private val userRepository: UserRepository) : GetMemberProfilesUseCase {

    override suspend operator fun invoke(userIds: List<String>): Map<String, User> = userRepository.getUsersByIds(
        userIds
    )
}
