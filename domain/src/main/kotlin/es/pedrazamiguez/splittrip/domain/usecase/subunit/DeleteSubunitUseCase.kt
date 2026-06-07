package es.pedrazamiguez.splittrip.domain.usecase.subunit

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DeleteSubunitUseCase : UseCase {
    suspend operator fun invoke(groupId: String, subunitId: String)
}
