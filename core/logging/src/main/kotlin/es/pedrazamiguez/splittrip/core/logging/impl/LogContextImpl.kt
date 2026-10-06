package es.pedrazamiguez.splittrip.core.logging.impl

import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.core.logging.sanitizer.hashIdentifier
import java.util.UUID

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
        get() = userIdProvider()?.hashIdentifier() ?: "anonymous"
}
