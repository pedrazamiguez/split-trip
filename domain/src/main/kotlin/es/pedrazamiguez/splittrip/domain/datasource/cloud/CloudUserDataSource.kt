package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.User

interface CloudUserDataSource {
    suspend fun saveUser(user: User)
    suspend fun getUsersByIds(userIds: List<String>): List<User>

    /**
     * Searches for users by email address (exact match).
     *
     * @param email The email address to search for
     * @param excludeUserId Optional user ID to exclude from results (e.g., the current user)
     * @return List of matching users
     */
    suspend fun searchUsersByEmail(email: String, excludeUserId: String? = null): List<User>
    suspend fun updateUserProfile(userId: String, displayName: String?, bio: String?, avatarUrl: String?)
}
