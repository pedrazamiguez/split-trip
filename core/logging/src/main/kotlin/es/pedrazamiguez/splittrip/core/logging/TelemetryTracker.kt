package es.pedrazamiguez.splittrip.core.logging

interface TelemetryTracker {
    fun trackScreenView(screenName: String, className: String?)
    fun trackEvent(eventName: String, params: Map<String, Any> = emptyMap())
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
}
