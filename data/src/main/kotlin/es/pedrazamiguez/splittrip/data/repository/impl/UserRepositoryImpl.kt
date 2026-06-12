package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class UserRepositoryImpl(
    private val cloudUserDataSource: CloudUserDataSource,
    private val localUserDataSource: LocalUserDataSource,
    private val cloudStorageDataSource: CloudStorageDataSource,
    private val authenticationService: AuthenticationService,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    override suspend fun saveUser(user: User): Result<Unit> {
        val localResult = runCatching { localUserDataSource.saveUsers(listOf(user)) }
        if (localResult.isFailure) {
            return localResult
        }
        return runCatching {
            cloudUserDataSource.saveUser(user)
        }.onFailure { e ->
            Timber.w(e, "Failed to save user profile to cloud, local cache was updated")
        }
    }

    override suspend fun saveGoogleUser(user: User): Result<Unit> = saveUser(user)

    override suspend fun getCurrentUserProfile(): User? {
        val userId = authenticationService.currentUserId() ?: return null

        // First try the generic local-first path
        val localUser = getUsersByIds(listOf(userId))[userId]

        // If we have a local user and its syncStatus is not SYNCED, trigger a background retry
        if (localUser != null && localUser.syncStatus != SyncStatus.SYNCED) {
            triggerBackgroundSync(userId, localUser.displayName, localUser.bio, localUser.profileImagePath)
        }

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

    override suspend fun updateUserProfile(
        userId: String,
        displayName: String?,
        bio: String?,
        localAvatarUri: String?
    ): Result<Unit> {
        return runCatching {
            // 1. Save locally first.
            val existingUser = localUserDataSource.getUsersByIds(listOf(userId)).firstOrNull()
                ?: error("Local user profile not found for ID: $userId")
            val updatedUser = existingUser.copy(
                displayName = displayName,
                profileImagePath = localAvatarUri,
                bio = bio,
                syncStatus = SyncStatus.PENDING_SYNC
            )
            localUserDataSource.saveUsers(listOf(updatedUser))

            // 2. Trigger background sync.
            triggerBackgroundSync(userId, displayName, bio, localAvatarUri)
        }
    }

    private fun triggerBackgroundSync(
        userId: String,
        displayName: String?,
        bio: String?,
        localAvatarUri: String?
    ) {
        syncCreateToCloud(
            scope = syncScope,
            entityId = userId,
            cloudWrite = {
                val avatarUrl = if (localAvatarUri != null) {
                    if (!localAvatarUri.startsWith("http")) {
                        cloudStorageDataSource.uploadAvatar(userId, localAvatarUri, "image/webp")
                    } else {
                        localAvatarUri
                    }
                } else {
                    cloudStorageDataSource.deleteAvatar(userId)
                    null
                }
                cloudUserDataSource.updateUserProfile(userId, displayName, bio, avatarUrl)

                // Update local DB with new remote avatar URL and SYNCED status
                val currentUser = localUserDataSource.getUsersByIds(listOf(userId)).firstOrNull()
                if (currentUser != null) {
                    val finalUser = currentUser.copy(
                        displayName = displayName,
                        bio = bio,
                        profileImagePath = avatarUrl,
                        syncStatus = SyncStatus.SYNCED
                    )
                    localUserDataSource.saveUsers(listOf(finalUser))
                }
            },
            updateSyncStatus = localUserDataSource::updateSyncStatus,
            getCurrentSyncStatus = { id ->
                localUserDataSource.getUsersByIds(listOf(id)).firstOrNull()?.syncStatus ?: SyncStatus.PENDING_SYNC
            },
            entityLabel = "user profile"
        )
    }

    override fun observeCurrentUserProfile(): Flow<User?> = flow {
        val userId = authenticationService.currentUserId()
        if (userId != null) {
            emitAll(localUserDataSource.observeUser(userId))
        } else {
            emit(null)
        }
    }
}
