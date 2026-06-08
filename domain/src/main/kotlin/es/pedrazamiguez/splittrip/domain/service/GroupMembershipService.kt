package es.pedrazamiguez.splittrip.domain.service

interface GroupMembershipService {
    suspend fun requireMembership(groupId: String)
    suspend fun requireUserInGroup(groupId: String, userId: String)
}
