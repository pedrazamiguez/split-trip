package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface ArchiveGroupUseCase : UseCase {
    suspend operator fun invoke(groupId: String): Result<Unit>
}
