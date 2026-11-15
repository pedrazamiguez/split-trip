package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetNonRemovableGroupMembersUseCase : UseCase {
    suspend operator fun invoke(groupId: String): Map<String, CannotRemoveMemberException.Reason>
}
