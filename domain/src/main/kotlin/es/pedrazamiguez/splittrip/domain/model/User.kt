package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import java.security.MessageDigest
import java.time.LocalDateTime

data class User(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val profileImagePath: String? = null,
    val bio: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: LocalDateTime? = null,
    val isPending: Boolean = false
) {
    companion object {
        fun generatePendingUserId(email: String): String {
            val trimmedEmail = email.trim().lowercase()
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(trimmedEmail.toByteArray(Charsets.UTF_8))
            val hashString = hash.joinToString("") { "%02x".format(it) }
            return "pending_$hashString"
        }
    }
}
