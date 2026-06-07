package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService

class GroupMembershipServiceImpl(
    private val groupRepository: GroupRepository,
    private val authenticationService: AuthenticationService
) : GroupMembershipService {

    /**
     * Verifies that the current authenticated user is a member of the specified group.
     *
     * @param groupId The group to validate membership against.
     * @throws NotGroupMemberException if the user is not a member or the group does not exist.
     * @throws IllegalStateException if no user is authenticated.
     */
    override suspend fun requireMembership(groupId: String) {
        val userId = authenticationService.requireUserId()
        requireUserInGroup(groupId, userId)
    }

    /**
     * Verifies that a specific user is a member of the specified group.
     *
     * Used when validating a **target member** (e.g., during impersonation)
     * independently of the authenticated user.
     *
     * @param groupId The group to validate membership against.
     * @param userId The user ID to check.
     * @throws NotGroupMemberException if the user is not a member or the group does not exist.
     */
    override suspend fun requireUserInGroup(groupId: String, userId: String) {
        val group = groupRepository.getGroupById(groupId)
            ?: throw NotGroupMemberException(groupId = groupId, userId = userId)

        if (userId !in group.members) {
            throw NotGroupMemberException(groupId = groupId, userId = userId)
        }
    }
}
