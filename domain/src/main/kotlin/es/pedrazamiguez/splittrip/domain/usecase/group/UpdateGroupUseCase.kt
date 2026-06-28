package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UpdateGroupUseCase : UseCase {
    suspend operator fun invoke(group: Group): Result<Unit>
}
