package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface ObserveGroupUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<Group?>
}
