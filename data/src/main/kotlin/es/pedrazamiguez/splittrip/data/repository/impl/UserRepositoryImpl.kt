package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import timber.log.Timber

class UserRepositoryImpl(
    private val cloudUserDataSource: CloudUserDataSource,
    private val localUserDataSource: LocalUserDataSource,
    private val authenticationService: AuthenticationService
) : UserRepository {

    override suspend fun saveUser(user: User): Result<Unit> = runCatching {
        cloudUserDataSource.saveUser(user)
        // Also cache locally so the current user's display name is
        // available offline immediately without a Firestore round-trip.
        localUserDataSource.saveUsers(listOf(user))
    }

    override suspend fun saveGoogleUser(user: User): Result<Unit> = saveUser(user)

    override suspend fun getCurrentUserProfile(): User? {
        val userId = authenticationService.currentUserId() ?: return null

        // First try the generic local-first path
        val localUser = getUsersByIds(listOf(userId))[userId]

        // If we have a fully-populated profile (including createdAt), return it
        if (localUser != null && localUser.createdAt != null) {
            return localUser
        }

        // Otherwise the local row is missing required fields (e.g. createdAt was
        // not available when saveGoogleUser cached the profile).
        // Force a cloud refresh and upsert into Room before returning.
        var profile = try {
            val refreshedUsers = cloudUserDataSource.getUsersByIds(listOf(userId))
            if (refreshedUsers.isNotEmpty()) {
                localUserDataSource.saveUsers(refreshedUsers)
                refreshedUsers.firstOrNull()
            } else {
                localUser
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh current user profile from cloud")
            localUser
        }

        if (profile != null && profile.createdAt == null) {
            val creationTimestamp = authenticationService.getCurrentUserCreationTimestamp()
            if (creationTimestamp != null) {
                val resolvedCreatedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTimestamp), ZoneOffset.UTC)
                val updatedProfile = profile.copy(createdAt = resolvedCreatedAt)
                saveUser(updatedProfile)
                profile = updatedProfile
            }
        }

        return profile
    }

    override suspend fun getUsersByIds(userIds: List<String>): Map<String, User> {
        if (userIds.isEmpty()) return emptyMap()

        // 1. Try local (Room) first
        val localUsers = localUserDataSource.getUsersByIds(userIds)
        val localMap = localUsers.associateBy { it.userId }

        // 2. Identify missing IDs
        val missingIds = userIds.filter { it !in localMap }

        // 3. Fetch missing from cloud, cache locally
        if (missingIds.isNotEmpty()) {
            try {
                val cloudUsers = cloudUserDataSource.getUsersByIds(missingIds)
                if (cloudUsers.isNotEmpty()) {
                    localUserDataSource.saveUsers(cloudUsers)
                }
                return (localUsers + cloudUsers).associateBy { it.userId }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch missing user profiles from cloud")
            }
        }

        return localMap
    }

    override suspend fun searchUsersByEmail(email: String): List<User> {
        val currentUserId = authenticationService.currentUserId()
        return cloudUserDataSource.searchUsersByEmail(email, excludeUserId = currentUserId)
    }
}
