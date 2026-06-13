package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.BuildConfig
import es.pedrazamiguez.splittrip.MainActivity
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.provider.IntentProvider
import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.core.logging.impl.LogContextImpl
import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.provider.impl.AppMetadataProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.IntentProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.LocaleProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.ResourceProviderImpl
import java.util.UUID
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single<AppMetadataProvider> { AppMetadataProviderImpl(context = androidContext()) }
    single<IntentProvider> {
        IntentProviderImpl(targetActivityClassName = MainActivity::class.java.name)
    }
    single<LocaleProvider> { LocaleProviderImpl(context = androidContext()) }
    single<ResourceProvider> { ResourceProviderImpl(context = androidContext()) }
    single<LogContext> {
        val authService = get<AuthenticationService>()
        val userPreferences = get<UserPreferences>()
        // Preloaded async so the provider lambda never blocks the main thread at Timber init.
        val deviceIdDeferred: Deferred<String> = MainScope().async(Dispatchers.IO) {
            val currentId = userPreferences.deviceId.first()
            if (currentId != null) {
                currentId
            } else {
                val newId = UUID.randomUUID().toString()
                userPreferences.setDeviceId(newId)
                newId
            }
        }
        LogContextImpl(
            appVersion = BuildConfig.VERSION_NAME,
            deviceIdProvider = { runBlocking { deviceIdDeferred.await() } },
            userIdProvider = { authService.currentUserId() }
        )
    }
}
