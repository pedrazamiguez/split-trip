package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface AddGroupMembersUseCase : UseCase {
    suspend operator fun invoke(groupId: String, newMembers: List<User>): Result<Unit>
}
