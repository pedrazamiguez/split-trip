package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.coroutines.flow.Flow

interface LocalUserDataSource {
    suspend fun saveUsers(users: List<User>)
    suspend fun getUsersByIds(userIds: List<String>): List<User>
    suspend fun updateSyncStatus(userId: String, syncStatus: SyncStatus)
    fun observeUser(userId: String): Flow<User?>
    suspend fun deleteUser(userId: String)
}
