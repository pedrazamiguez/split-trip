package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.User

interface LocalUserDataSource {
    suspend fun saveUsers(users: List<User>)
    suspend fun getUsersByIds(userIds: List<String>): List<User>
    suspend fun updateSyncStatus(userId: String, syncStatus: SyncStatus)
}
