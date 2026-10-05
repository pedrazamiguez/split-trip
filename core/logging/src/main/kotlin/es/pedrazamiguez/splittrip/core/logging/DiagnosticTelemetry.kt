package es.pedrazamiguez.splittrip.core.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Records a non-fatal exception to Firebase Crashlytics with contextual diagnostic keys.
 * Safe to call in test/development environments: errors during telemetry dispatch are caught and ignored.
 */
fun recordDiagnosticException(
    throwable: Throwable,
    customKeys: Map<String, String> = emptyMap()
) {
    runCatching {
        val crashlytics = FirebaseCrashlytics.getInstance()
        customKeys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
        crashlytics.recordException(throwable)
    }
}
