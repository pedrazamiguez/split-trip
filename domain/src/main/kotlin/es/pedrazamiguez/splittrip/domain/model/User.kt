package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
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
    val isPending: Boolean = false,
    val timezone: String? = null,
    val preferredReminderTime: String? = null,
    val tier: SubscriptionTier = SubscriptionTier.FREE
) {
    val isPro: Boolean get() = tier == SubscriptionTier.PRO

    companion object {
        fun normalizeEmail(email: String): String = email.trim().lowercase()

        fun canonicalizeEmail(email: String): String {
            val cleanEmail = email.trim().lowercase()
            val parts = cleanEmail.split("@")
            if (parts.size != 2) return cleanEmail
            val localPart = parts[0]
            val domain = parts[1]

            if (domain == "gmail.com" || domain == "googlemail.com") {
                val baseLocal = localPart.substringBefore("+").replace(".", "")
                return "$baseLocal@gmail.com"
            }
            return cleanEmail
        }

        fun areEmailsEquivalent(first: String, second: String): Boolean {
            return canonicalizeEmail(first) == canonicalizeEmail(second)
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
