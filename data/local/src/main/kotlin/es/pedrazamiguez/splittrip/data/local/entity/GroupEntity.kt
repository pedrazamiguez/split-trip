package es.pedrazamiguez.splittrip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Group stored locally.
 * This is part of the Offline-First strategy where the local database
 * serves as the Single Source of Truth for the UI.
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val currencyCode: String,
    val extraCurrencies: List<String>,
    val memberIds: List<String>,
    val mainImagePath: String?,
    val createdAtMillis: Long?,
    val lastUpdatedAtMillis: Long?,
    val syncStatus: String = "SYNCED",
    val status: String = "ACTIVE",
    val createdBy: String = ""
)
