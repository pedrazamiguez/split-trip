package es.pedrazamiguez.splittrip.core.logging.di

import es.pedrazamiguez.splittrip.core.logging.BuildConfig
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import es.pedrazamiguez.splittrip.core.logging.impl.DebugTelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.impl.FirebaseTelemetryTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreLoggingModule = module {
    single<RollingLogBuffer> { RollingLogBuffer() }

    single<TelemetryTracker> {
        createTelemetryTracker(
            isDebug = BuildConfig.DEBUG,
            firebaseTrackerFactory = { FirebaseTelemetryTracker(context = androidContext()) }
        )
    }
}

internal fun createTelemetryTracker(
    isDebug: Boolean,
    firebaseTrackerFactory: () -> TelemetryTracker
): TelemetryTracker = if (isDebug) {
    DebugTelemetryTracker(firebaseTracker = null)
} else {
    firebaseTrackerFactory()
}
