package es.pedrazamiguez.splittrip.features.settings.di

import android.app.Application
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.CloudMetadataService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.AccountStatusUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.AccountStatusScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DefaultCurrencyScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DeveloperServicesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.LanguageScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.NotificationPreferencesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.SettingsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.ThemeScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AccountStatusViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AppVersionViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DefaultCurrencyViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.InstallationIdViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.LanguageViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.NotificationPreferencesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SettingsViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ThemeViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandler
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsUiModule = module {

    viewModel {
        SettingsViewModel(
            signOutUseCase = get<SignOutUseCase>(),
            getUserDefaultCurrencyUseCase = get<GetUserDefaultCurrencyUseCase>(),
            getAppLanguageUseCase = get<GetAppLanguageUseCase>(),
            getShouldShowLanguagePillUseCase = get<GetShouldShowLanguagePillUseCase>(),
            consumeLanguagePillUseCase = get<ConsumeLanguagePillUseCase>(),
            getAppThemeUseCase = get<GetAppThemeUseCase>()
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
        LanguageViewModel(
            getAppLanguageUseCase = get<GetAppLanguageUseCase>(),
            setAppLanguageUseCase = get<SetAppLanguageUseCase>()
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
            aiModelResolver = get<AiModelResolverService>()
        )
    }

    viewModel {
        ThemeViewModel(
            getAppThemeUseCase = get<GetAppThemeUseCase>(),
            setAppThemeUseCase = get<SetAppThemeUseCase>()
        )
    }

    factory<AccountStatusUiMapper> { AccountStatusUiMapperImpl(localeProvider = get()) }

    factory<AccountStatusEventHandler> {
        AccountStatusEventHandlerImpl(
            getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>(),
            getLinkedProvidersUseCase = get<GetLinkedProvidersUseCase>(),
            linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>(),
            linkEmailPasswordUseCase = get<LinkEmailPasswordUseCase>(),
            unlinkProviderUseCase = get<UnlinkProviderUseCase>(),
            accountStatusUiMapper = get<AccountStatusUiMapper>()
        )
    }

    viewModel {
        AccountStatusViewModel(
            accountStatusEventHandler = get<AccountStatusEventHandler>()
        )
    }

    single { DefaultCurrencyScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { LanguageScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { NotificationPreferencesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { SettingsScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { DeveloperServicesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { ThemeScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { AccountStatusScreenUiProviderImpl() } bind ScreenUiProvider::class
}
