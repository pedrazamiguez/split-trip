package es.pedrazamiguez.splittrip.core.logging

interface LogContext {
    val userId: String
    val deviceId: String
    val appVersion: String
    val sessionId: String
}
