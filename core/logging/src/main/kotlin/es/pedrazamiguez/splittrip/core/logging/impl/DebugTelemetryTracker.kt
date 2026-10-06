package es.pedrazamiguez.splittrip.core.logging.impl

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import timber.log.Timber

class DebugTelemetryTracker(
    private val firebaseTracker: TelemetryTracker? = null
) : TelemetryTracker {
    override fun trackScreenView(screenName: String, className: String?) {
        val classInfo = className?.let { " (class: $it)" } ?: ""
        Timber.tag(LogTag.TELEMETRY).d("Screen View: $screenName$classInfo")
        firebaseTracker?.trackScreenView(screenName, className)
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>) {
        val paramsStr = if (params.isNotEmpty()) " | Params: $params" else ""
        Timber.tag(LogTag.TELEMETRY).d("Event: $eventName$paramsStr")
        firebaseTracker?.trackEvent(eventName, params)
    }

    override fun setUserId(userId: String?) {
        Timber.tag(LogTag.TELEMETRY).d("User ID set to: $userId")
        firebaseTracker?.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        Timber.tag(LogTag.TELEMETRY).d("User Property: $name = $value")
        firebaseTracker?.setUserProperty(name, value)
    }

    override fun setCustomKey(key: String, value: String) {
        Timber.tag(LogTag.TELEMETRY).d("Custom Key: $key = $value")
        firebaseTracker?.setCustomKey(key, value)
    }

    override fun setCustomKey(key: String, value: Boolean) {
        Timber.tag(LogTag.TELEMETRY).d("Custom Key: $key = $value")
        firebaseTracker?.setCustomKey(key, value)
    }
}
