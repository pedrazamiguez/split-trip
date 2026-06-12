package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import java.time.LocalDateTime

data class User(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val profileImagePath: String? = null,
    val bio: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: LocalDateTime? = null
)
