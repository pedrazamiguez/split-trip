package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface CreateGroupUseCase : UseCase {
    suspend operator fun invoke(group: Group, members: List<User>): Result<String>
}
