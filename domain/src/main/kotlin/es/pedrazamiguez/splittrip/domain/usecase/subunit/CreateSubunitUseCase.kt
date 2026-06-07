package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface CreateSubunitUseCase : UseCase {
    suspend operator fun invoke(groupId: String, subunit: Subunit): Result<String>
}
