package es.pedrazamiguez.splittrip.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class ProductionCrashlyticsTree(
    private val logContext: LogContext
) : Timber.Tree() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        crashlytics.setCustomKey("deviceId", logContext.deviceId)
        crashlytics.setCustomKey("sessionId", logContext.sessionId)
        crashlytics.setCustomKey("appVersion", logContext.appVersion)
        crashlytics.setUserId(logContext.userId)
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.setUserId(logContext.userId)

        if (priority < Log.WARN) return

        crashlytics.log("${priorityToString(priority)}/$tag: $message")
        t?.let { crashlytics.recordException(it) }
    }

    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }
}
