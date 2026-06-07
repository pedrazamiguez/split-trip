package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.BuildConfig
import es.pedrazamiguez.splittrip.MainActivity
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.provider.IntentProvider
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.logging.LogContext
import es.pedrazamiguez.splittrip.logging.LogContextImpl
import es.pedrazamiguez.splittrip.provider.impl.AppMetadataProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.IntentProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.LocaleProviderImpl
import es.pedrazamiguez.splittrip.provider.impl.ResourceProviderImpl
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
        LogContextImpl(
            context = androidContext(),
            appVersion = BuildConfig.VERSION_NAME,
            userIdProvider = { authService.currentUserId() }
        )
    }
}
