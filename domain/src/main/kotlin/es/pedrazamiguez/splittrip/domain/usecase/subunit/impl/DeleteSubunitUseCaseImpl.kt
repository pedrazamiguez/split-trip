package es.pedrazamiguez.splittrip.domain.usecase.subunit.impl

import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase

class DeleteSubunitUseCaseImpl(
    private val subunitRepository: SubunitRepository,
    private val groupMembershipService: GroupMembershipService
) : DeleteSubunitUseCase {

    /**
     * Deletes a subunit by its ID within a group.
     *
     * @param groupId The ID of the group containing the subunit.
     * @param subunitId The ID of the subunit to delete.
     * @throws NotGroupMemberException if the user is not a member of the group.
     */
    override suspend operator fun invoke(groupId: String, subunitId: String) {
        groupMembershipService.requireMembership(groupId)
        subunitRepository.deleteSubunit(groupId, subunitId)
    }
}
