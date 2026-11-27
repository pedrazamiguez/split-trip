package es.pedrazamiguez.splittrip.core.logging.di

import es.pedrazamiguez.splittrip.core.logging.BuildConfig
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import es.pedrazamiguez.splittrip.core.logging.exit.AndroidProcessExitInfoProvider
import es.pedrazamiguez.splittrip.core.logging.exit.LastReportedExitTimestampStore
import es.pedrazamiguez.splittrip.core.logging.exit.ProcessExitDiagnosticsReporter
import es.pedrazamiguez.splittrip.core.logging.exit.ProcessExitInfoProvider
import es.pedrazamiguez.splittrip.core.logging.exit.SharedPreferencesExitTimestampStore
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

    single<ProcessExitInfoProvider> {
        AndroidProcessExitInfoProvider(context = androidContext())
    }

    single<LastReportedExitTimestampStore> {
        SharedPreferencesExitTimestampStore(context = androidContext())
    }

    single<ProcessExitDiagnosticsReporter> {
        val processExitInfoProvider = get<ProcessExitInfoProvider>()
        val lastReportedExitTimestampStore = get<LastReportedExitTimestampStore>()
        ProcessExitDiagnosticsReporter(
            exitInfoProvider = processExitInfoProvider,
            timestampStore = lastReportedExitTimestampStore
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
