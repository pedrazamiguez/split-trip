package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.BuildConfig
import es.pedrazamiguez.splittrip.MainActivity
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.provider.IntentProvider
import es.pedrazamiguez.splittrip.core.logging.LogContext
import es.pedrazamiguez.splittrip.core.logging.LogContextImpl
import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.provider.impl.AppMetadataProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.IntentProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.LocaleProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.ResourceProviderImpl
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
        LogContextImpl(
            appVersion = BuildConfig.VERSION_NAME,
            deviceIdProvider = {
                val flow = userPreferences.deviceId
                runBlocking {
                    val currentId = flow.first()
                    if (currentId != null) {
                        currentId
                    } else {
                        val newId = java.util.UUID.randomUUID().toString()
                        userPreferences.setDeviceId(newId)
                        newId
                    }
                }
            },
            userIdProvider = { authService.currentUserId() }
        )
    }
}
