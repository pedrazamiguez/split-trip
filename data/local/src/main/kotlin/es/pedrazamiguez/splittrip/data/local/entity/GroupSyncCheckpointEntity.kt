package es.pedrazamiguez.splittrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_sync_checkpoints",
    primaryKeys = ["groupId", "collectionName"],
    indices = [Index(value = ["groupId"])],
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupSyncCheckpointEntity(
    val groupId: String,
    val collectionName: String,
    val lastServerUpdatedSeconds: Long,
    val lastServerUpdatedNanos: Int,
    val lastSyncedAtClientMillis: Long
)
