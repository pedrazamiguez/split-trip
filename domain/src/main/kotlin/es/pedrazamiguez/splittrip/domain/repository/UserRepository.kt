package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.User

interface UserRepository {
    suspend fun saveGoogleUser(user: User): Result<Unit>

    /**
     * Saves a user profile to the cloud database and caches it locally.
     */
    suspend fun saveUser(user: User): Result<Unit>

    /**
     * Returns the current authenticated user's profile.
     *
     * Checks the local cache (Room) first, then fetches from the cloud
     * if not found locally, and caches the result.
     */
    suspend fun getCurrentUserProfile(): User?

    /**
     * Returns a map of userId → [User] for the given IDs.
     *
     * Checks the local cache (Room) first, then fetches any missing users
     * from the cloud and caches them locally for future lookups.
     */
    suspend fun getUsersByIds(userIds: List<String>): Map<String, User>

    /**
     * Searches for users by email address (exact match).
     * This is a cloud-only lookup — results are not cached locally.
     *
     * @param email The email address to search for
     * @return List of matching users, excluding the current user
     */
    suspend fun searchUsersByEmail(email: String): List<User>
}
