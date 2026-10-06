package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import es.pedrazamiguez.splittrip.data.billing.PlayBillingClientWrapper
import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.data.repository.impl.BalancePreferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.DiagnosticLogRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.GroupPreferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.OnboardingPreferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.UserPreferenceRepositoryImpl
import es.pedrazamiguez.splittrip.data.service.BiometricAuthServiceImpl
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.DiagnosticLogRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.OnboardingPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.service.BiometricAuthService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsDataModule = module {
    single<OnboardingPreferenceRepository> {
        OnboardingPreferenceRepositoryImpl(userPreferences = get<UserPreferences>())
    }

    single<GroupPreferenceRepository> {
        GroupPreferenceRepositoryImpl(userPreferences = get<UserPreferences>())
    }

    single<UserPreferenceRepository> {
        UserPreferenceRepositoryImpl(userPreferences = get<UserPreferences>())
    }

    single<BalancePreferenceRepository> {
        BalancePreferenceRepositoryImpl(userPreferences = get<UserPreferences>())
    }

    single<DiagnosticLogRepository> {
        val rollingLogBuffer = get<RollingLogBuffer>()
        DiagnosticLogRepositoryImpl(rollingLogBuffer = rollingLogBuffer)
    }

    single<BiometricAuthService> {
        BiometricAuthServiceImpl(context = androidContext())
    }

    single<BillingService> {
        val appConfigRepository = get<AppConfigRepository>()
        PlayBillingClientWrapper(
            context = androidContext(),
            appConfigRepository = appConfigRepository
        )
    }
}
