package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetMemberProfilesUseCase : UseCase {
    suspend operator fun invoke(userIds: List<String>): Map<String, User>
}
