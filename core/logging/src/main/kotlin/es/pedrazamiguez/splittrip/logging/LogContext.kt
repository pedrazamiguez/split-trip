package es.pedrazamiguez.splittrip.logging

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

interface LogContext {
    val userId: String
    val deviceId: String
    val appVersion: String
    val sessionId: String
}

class LogContextImpl(
    private val context: Context,
    override val appVersion: String,
    private val userIdProvider: () -> String?
) : LogContext {
    override val sessionId: String = UUID.randomUUID().toString()

    override val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("logging_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        id
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
