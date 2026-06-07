package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UpdateSubunitUseCase : UseCase {
    suspend operator fun invoke(groupId: String, subunit: Subunit): Result<Unit>
}
