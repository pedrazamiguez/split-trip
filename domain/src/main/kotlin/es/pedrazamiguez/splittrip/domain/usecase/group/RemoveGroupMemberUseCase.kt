package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface RemoveGroupMemberUseCase : UseCase {
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit>
}
