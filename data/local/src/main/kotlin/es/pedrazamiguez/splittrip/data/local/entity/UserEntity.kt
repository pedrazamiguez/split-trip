package es.pedrazamiguez.splittrip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val displayName: String?,
    val profileImagePath: String?,
    val createdAtMillis: Long?,
    val lastUpdatedAtMillis: Long?,
    val bio: String? = null,
    val syncStatus: String = "SYNCED"
)
