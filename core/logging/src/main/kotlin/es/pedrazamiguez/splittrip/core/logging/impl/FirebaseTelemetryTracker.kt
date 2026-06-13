package es.pedrazamiguez.splittrip.core.logging.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker

@SuppressLint("MissingPermission")
class FirebaseTelemetryTracker(context: Context) : TelemetryTracker {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun trackScreenView(screenName: String, className: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            className?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
