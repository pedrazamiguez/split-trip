package es.pedrazamiguez.splittrip.features.settings.di

import android.app.Application
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.CloudMetadataService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DefaultCurrencyScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DeveloperServicesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.NotificationPreferencesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.SettingsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AppVersionViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DefaultCurrencyViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.InstallationIdViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.NotificationPreferencesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsUiModule = module {

    viewModel {
        SettingsViewModel(
            signOutUseCase = get<SignOutUseCase>(),
            getUserDefaultCurrencyUseCase = get<GetUserDefaultCurrencyUseCase>()
        )
    }

    viewModel { InstallationIdViewModel(cloudMetadataService = get<CloudMetadataService>()) }
    viewModel { AppVersionViewModel(application = get<Application>()) }

    viewModel {
        DefaultCurrencyViewModel(
            getUserDefaultCurrencyUseCase = get<GetUserDefaultCurrencyUseCase>(),
            setUserDefaultCurrencyUseCase = get<SetUserDefaultCurrencyUseCase>()
        )
    }

    viewModel {
        NotificationPreferencesViewModel(
            getNotificationPreferencesUseCase = get<GetNotificationPreferencesUseCase>(),
            updateNotificationPreferenceUseCase = get<UpdateNotificationPreferenceUseCase>()
        )
    }

    viewModel {
        DeveloperServicesViewModel(
            receiptOcrService = get<ReceiptOcrService>(),
            receiptExtractionService = get<ReceiptExtractionService>(),
            userPreferenceRepository = get<es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository>()
        )
    }

    single { DefaultCurrencyScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { NotificationPreferencesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { SettingsScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { DeveloperServicesScreenUiProviderImpl() } bind ScreenUiProvider::class
}
