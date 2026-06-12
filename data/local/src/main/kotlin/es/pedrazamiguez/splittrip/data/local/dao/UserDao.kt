package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.UserEntity

@Dao
interface UserDao {

    @Upsert
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM users WHERE userId IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<UserEntity>

    @Query("UPDATE users SET syncStatus = :syncStatus WHERE userId = :userId")
    suspend fun updateSyncStatus(userId: String, syncStatus: String)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}
