package es.pedrazamiguez.splittrip.core.logging.di

import es.pedrazamiguez.splittrip.core.logging.BuildConfig
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.impl.DebugTelemetryTracker
import es.pedrazamiguez.splittrip.core.logging.impl.FirebaseTelemetryTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreLoggingModule = module {
    single<TelemetryTracker> {
        if (BuildConfig.DEBUG) {
            DebugTelemetryTracker()
        } else {
            FirebaseTelemetryTracker(context = androidContext())
        }
    }
}
