package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetGroupSubunitsFlowUseCase : UseCase {
    operator fun invoke(groupId: String): Flow<List<Subunit>>
}
