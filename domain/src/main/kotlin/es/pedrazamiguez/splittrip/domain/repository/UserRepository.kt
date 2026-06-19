package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun saveGoogleUser(user: User): Result<Unit>

    /**
     * Saves a user profile to the remote data source and caches it locally.
     */
    suspend fun saveUser(user: User): Result<Unit>

    /**
     * Returns the current authenticated user's profile.
     *
     * Checks the local cache first, then fetches from the remote data source
     * if not found locally, and caches the result.
     */
    suspend fun getCurrentUserProfile(): User?

    /**
     * Returns a map of userId → [User] for the given IDs.
     *
     * Checks the local cache first, then fetches any missing users
     * from the remote data source and caches them locally for future lookups.
     */
    suspend fun getUsersByIds(userIds: List<String>): Map<String, User>

    /**
     * Searches for users by email address (exact match).
     * This is a remote-only lookup — results are not cached locally.
     *
     * @param email The email address to search for
     * @return List of matching users, excluding the current user
     */
    suspend fun searchUsersByEmail(email: String): List<User>

    /**
     * Updates the user's profile information (display name, bio, and/or avatar).
     */
    suspend fun updateUserProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        localAvatarUri: String?
    ): Result<Unit>

    /**
     * Observes the current authenticated user's profile.
     */
    fun observeCurrentUserProfile(): Flow<User?>
}
