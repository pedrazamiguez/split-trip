package es.pedrazamiguez.splittrip.core.logging.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.sanitizer.hashIdentifier

@SuppressLint("MissingPermission")
class FirebaseTelemetryTracker(
    context: Context,
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context),
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance(),
    private val bundleFactory: () -> Bundle = { Bundle() }
) : TelemetryTracker {

    override fun trackScreenView(screenName: String, className: String?) {
        val bundle = bundleFactory().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            className?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        crashlytics.setCustomKey("current_screen", screenName)
        crashlytics.log("Screen: $screenName")
    }

    override fun trackEvent(eventName: String, params: Map<String, Any>) {
        val bundle = bundleFactory().apply {
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
        val standardizedId = userId?.hashIdentifier()
        firebaseAnalytics.setUserId(standardizedId)
        crashlytics.setUserId(standardizedId ?: "anonymous")
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
}
