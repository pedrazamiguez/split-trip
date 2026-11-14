package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.sync.syncCreateToCloud
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

class UserRepositoryImpl(
    private val cloudUserDataSource: CloudUserDataSource,
    private val localUserDataSource: LocalUserDataSource,
    private val cloudStorageDataSource: CloudStorageDataSource,
    private val authenticationService: AuthenticationService,
    private val performanceMonitor: PerformanceMonitor,
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
            val existingUser = localUserDataSource.getUsersByIds(listOf(userId)).firstOrNull()
                ?: error("Local user profile not found for ID: $userId")
            val updatedUser = existingUser.copy(
                displayName = displayName,
                profileImagePath = localAvatarUri,
                bio = bio,
                syncStatus = SyncStatus.PENDING_SYNC
            )
            localUserDataSource.saveUsers(listOf(updatedUser))

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
            entityLabel = "user profile",
            performanceMonitor = performanceMonitor
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUserProfile(): Flow<User?> =
        authenticationService.authState
            .onStart { emit(authenticationService.currentUserId() != null) }
            .map { isLoggedIn -> if (isLoggedIn) authenticationService.currentUserId() else null }
            .distinctUntilChanged()
            .flatMapLatest { userId ->
                if (userId != null) {
                    localUserDataSource.observeUser(userId)
                } else {
                    flowOf(null)
                }
            }
            .distinctUntilChanged()

    override suspend fun deletePendingUser(userId: String): Result<Unit> = runCatching {
        localUserDataSource.deleteUser(userId)
        cloudUserDataSource.deleteUser(userId)
    }

    override suspend fun updateUserReminderPreferences(
        userId: String,
        timezone: String?,
        preferredReminderTime: String?
    ): Result<Unit> {
        return runCatching {
            localUserDataSource.updateUserReminderPreferences(userId, timezone, preferredReminderTime)

            syncCreateToCloud(
                scope = syncScope,
                entityId = userId,
                cloudWrite = {
                    cloudUserDataSource.updateUserReminderPreferences(userId, timezone, preferredReminderTime)
                },
                updateSyncStatus = localUserDataSource::updateSyncStatus,
                getCurrentSyncStatus = { id ->
                    localUserDataSource.getUsersByIds(listOf(id)).firstOrNull()?.syncStatus ?: SyncStatus.PENDING_SYNC
                },
                entityLabel = "user reminder preferences",
                performanceMonitor = performanceMonitor
            )
        }
    }

    override suspend fun updateUserTier(userId: String, tier: SubscriptionTier): Result<Unit> {
        return runCatching {
            localUserDataSource.updateUserTier(userId, tier, SyncStatus.PENDING_SYNC)

            syncCreateToCloud(
                scope = syncScope,
                entityId = userId,
                cloudWrite = {
                    cloudUserDataSource.updateUserTier(userId, tier)
                },
                updateSyncStatus = localUserDataSource::updateSyncStatus,
                getCurrentSyncStatus = { id ->
                    localUserDataSource.getUsersByIds(listOf(id)).firstOrNull()?.syncStatus ?: SyncStatus.PENDING_SYNC
                },
                entityLabel = "user tier",
                performanceMonitor = performanceMonitor
            )
        }
    }
}
