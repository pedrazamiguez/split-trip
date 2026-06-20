package es.pedrazamiguez.splittrip.data.local.datasource.impl

import es.pedrazamiguez.splittrip.data.local.dao.UserDao
import es.pedrazamiguez.splittrip.data.local.mapper.toDomain
import es.pedrazamiguez.splittrip.data.local.mapper.toEntities
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalUserDataSourceImpl(private val userDao: UserDao) : LocalUserDataSource {

    override suspend fun saveUsers(users: List<User>) {
        userDao.insertUsers(users.toEntities())
    }

    override suspend fun getUsersByIds(userIds: List<String>): List<User> = userDao.getUsersByIds(userIds).toDomain()

    override suspend fun updateSyncStatus(userId: String, syncStatus: SyncStatus) {
        userDao.updateSyncStatus(userId, syncStatus.name)
    }

    override fun observeUser(userId: String): Flow<User?> {
        return userDao.observeUserById(userId).map { it?.toDomain() }
    }

    override suspend fun deleteUser(userId: String) {
        userDao.deleteUserById(userId)
    }
}
