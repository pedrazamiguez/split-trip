package es.pedrazamiguez.splittrip.core.logging.tree

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.core.logging.exception.SyntheticDiagnosticException
import es.pedrazamiguez.splittrip.core.logging.sanitizer.sanitizePii
import timber.log.Timber

class ProductionCrashlyticsTree(
    private val logContext: LogContext,
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
) : Timber.Tree() {

    init {
        crashlytics.setCustomKey("deviceId", logContext.deviceId)
        crashlytics.setCustomKey("sessionId", logContext.sessionId)
        crashlytics.setCustomKey("appVersion", logContext.appVersion)
        crashlytics.setUserId(logContext.userId)
    }

    public override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.setUserId(logContext.userId)

        if (priority < Log.INFO) return

        val formattedTag = tag ?: "SplitTrip"
        val sanitizedMessage = message.sanitizePii()

        crashlytics.log("${priorityToString(priority)}/$formattedTag: $sanitizedMessage")

        if (priority >= Log.ERROR) {
            val exception = t ?: SyntheticDiagnosticException("[$formattedTag] $sanitizedMessage")
            crashlytics.recordException(exception)
        } else if (priority == Log.WARN && t != null) {
            crashlytics.recordException(t)
        }
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
