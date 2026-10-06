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
        val firebaseTracker = FirebaseTelemetryTracker(context = androidContext())
        if (BuildConfig.DEBUG) {
            DebugTelemetryTracker(firebaseTracker = firebaseTracker)
        } else {
            firebaseTracker
        }
    }
}
