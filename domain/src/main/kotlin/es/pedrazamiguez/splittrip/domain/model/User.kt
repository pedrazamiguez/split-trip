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
        fun normalizeEmail(email: String): String {
            val cleanEmail = email.trim().lowercase()
            val parts = cleanEmail.split("@")
            if (parts.size != 2) return cleanEmail
            val localPart = parts[0]
            val domain = parts[1]

            if (domain == "gmail.com" || domain == "googlemail.com") {
                val baseLocal = localPart.substringBefore("+")
                val normalizedLocal = baseLocal.replace(".", "")
                return "$normalizedLocal@$domain"
            }
            return cleanEmail
        }

        fun generatePendingUserId(email: String): String {
            val normalized = normalizeEmail(email)
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(normalized.toByteArray(Charsets.UTF_8))
            val hashString = hash.joinToString("") { "%02x".format(it) }
            return "pending_$hashString"
        }
    }
}
