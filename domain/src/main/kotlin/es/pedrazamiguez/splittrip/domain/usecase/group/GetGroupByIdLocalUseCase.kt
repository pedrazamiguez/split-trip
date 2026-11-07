package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetGroupByIdLocalUseCase : UseCase {
    suspend operator fun invoke(groupId: String): Group?
}
