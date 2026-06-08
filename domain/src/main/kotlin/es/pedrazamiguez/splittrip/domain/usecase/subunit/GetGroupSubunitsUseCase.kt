package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetGroupSubunitsUseCase : UseCase {
    suspend operator fun invoke(groupId: String): List<Subunit>
}
