package es.pedrazamiguez.splittrip.core.logging

import java.security.MessageDigest
import java.util.UUID

interface LogContext {
    val userId: String
    val deviceId: String
    val appVersion: String
    val sessionId: String
}

class LogContextImpl(
    override val appVersion: String,
    private val deviceIdProvider: () -> String,
    private val userIdProvider: () -> String?
) : LogContext {
    override val sessionId: String = UUID.randomUUID().toString()

    override val deviceId: String by lazy {
        deviceIdProvider()
    }

    override val userId: String
        get() = userIdProvider()?.hashOrAnonymous() ?: "anonymous"

    private fun String.hashOrAnonymous(): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(this.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "anonymous"
        }
    }
}
