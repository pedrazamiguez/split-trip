package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface DeleteGroupUseCase : UseCase {
    suspend operator fun invoke(groupId: String)
}
