package es.pedrazamiguez.splittrip.features.settings.di

import android.app.Application
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.BillingService
import es.pedrazamiguez.splittrip.domain.service.CloudMetadataService
import es.pedrazamiguez.splittrip.domain.service.PasswordValidationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.diagnostic.GetDiagnosticLogsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.CancelLocalAiModelPackDownloadUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ConsumeLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DeleteLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DownloadLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetDeveloperInfoUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetShouldShowLanguagePillUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveDeveloperAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObservePlanAllowancesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppLanguageUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetAppThemeUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLocalAiModelPackWifiOnlyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveProAllowlistAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileSubscriptionEntitlementsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserReminderPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountSecurityUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.DeveloperInfoUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.LocalAiModelPackUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.NotificationPreferencesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.PlanAllowancesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.SubscriptionsUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.AccountSecurityUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.AccountStatusUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.LocalAiModelPackUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.NotificationPreferencesUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.PlanAllowancesUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.SubscriptionsUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.AccountSecurityScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.AccountStatusScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DefaultCurrencyScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DeveloperInfoScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.DeveloperServicesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.FaqScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.LanguageScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.LocalAiModelPackScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.NotificationPreferencesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.OpenSourceScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.PlanAllowancesScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.PrivacyPolicyScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.SettingsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.SubscriptionsScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.impl.ThemeScreenUiProviderImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AccountSecurityViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AccountStatusViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AppVersionViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DefaultCurrencyViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperInfoViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.InstallationIdViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.LanguageViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.LocalAiModelPackViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.NotificationPreferencesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.PlanAllowancesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SettingsViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SubscriptionsUseCases
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SubscriptionsViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ThemeViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandler
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsUiModule = module {

    viewModel {
        val signOutUseCase = get<SignOutUseCase>()
        val getUserDefaultCurrencyUseCase = get<GetUserDefaultCurrencyUseCase>()
        val getAppLanguageUseCase = get<GetAppLanguageUseCase>()
        val getShouldShowLanguagePillUseCase = get<GetShouldShowLanguagePillUseCase>()
        val consumeLanguagePillUseCase = get<ConsumeLanguagePillUseCase>()
        val getAppThemeUseCase = get<GetAppThemeUseCase>()
        val isUserAnonymousUseCase = get<IsUserAnonymousUseCase>()
        val getDiagnosticLogsUseCase = get<GetDiagnosticLogsUseCase>()
        val observeDeveloperAccessUseCase = get<ObserveDeveloperAccessUseCase>()
        SettingsViewModel(
            signOutUseCase = signOutUseCase,
            getUserDefaultCurrencyUseCase = getUserDefaultCurrencyUseCase,
            getAppLanguageUseCase = getAppLanguageUseCase,
            getShouldShowLanguagePillUseCase = getShouldShowLanguagePillUseCase,
            consumeLanguagePillUseCase = consumeLanguagePillUseCase,
            getAppThemeUseCase = getAppThemeUseCase,
            isUserAnonymousUseCase = isUserAnonymousUseCase,
            getDiagnosticLogsUseCase = getDiagnosticLogsUseCase,
            observeDeveloperAccessUseCase = observeDeveloperAccessUseCase
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
        val getNotificationPreferencesUseCase = get<GetNotificationPreferencesUseCase>()
        val updateNotificationPreferenceUseCase = get<UpdateNotificationPreferenceUseCase>()
        val observeCurrentUserProfileUseCase = get<ObserveCurrentUserProfileUseCase>()
        val updateUserReminderPreferencesUseCase = get<UpdateUserReminderPreferencesUseCase>()
        val notificationPreferencesUiMapper = get<NotificationPreferencesUiMapper>()
        NotificationPreferencesViewModel(
            getNotificationPreferencesUseCase = getNotificationPreferencesUseCase,
            updateNotificationPreferenceUseCase = updateNotificationPreferenceUseCase,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
            updateUserReminderPreferencesUseCase = updateUserReminderPreferencesUseCase,
            notificationPreferencesUiMapper = notificationPreferencesUiMapper
        )
    }

    viewModel {
        val receiptOcrService = get<ReceiptOcrService>()
        val receiptExtractionService = get<ReceiptExtractionService>()
        val aiModelResolverService = get<AiModelResolverService>()
        val observeCurrentUserProfileUseCase = get<ObserveCurrentUserProfileUseCase>()
        val getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>()
        val updateUserTierUseCase = get<UpdateUserTierUseCase>()
        val observeDeveloperAccessUseCase = get<ObserveDeveloperAccessUseCase>()
        DeveloperServicesViewModel(
            receiptOcrService = receiptOcrService,
            receiptExtractionService = receiptExtractionService,
            aiModelResolver = aiModelResolverService,
            observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            updateUserTierUseCase = updateUserTierUseCase,
            observeDeveloperAccessUseCase = observeDeveloperAccessUseCase
        )
    }

    viewModel {
        val getAppThemeUseCase = get<GetAppThemeUseCase>()
        val setAppThemeUseCase = get<SetAppThemeUseCase>()
        ThemeViewModel(
            getAppThemeUseCase = getAppThemeUseCase,
            setAppThemeUseCase = setAppThemeUseCase
        )
    }

    factory<NotificationPreferencesUiMapper> { NotificationPreferencesUiMapperImpl() }

    factory<AccountStatusUiMapper> { AccountStatusUiMapperImpl(localeProvider = get()) }

    factory<AccountStatusEventHandler> {
        AccountStatusEventHandlerImpl(
            getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>(),
            getLinkedProvidersUseCase = get<GetLinkedProvidersUseCase>(),
            linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>(),
            linkEmailPasswordUseCase = get<LinkEmailPasswordUseCase>(),
            unlinkProviderUseCase = get<UnlinkProviderUseCase>(),
            isUserAnonymousUseCase = get<IsUserAnonymousUseCase>(),
            accountStatusUiMapper = get<AccountStatusUiMapper>(),
            passwordValidationService = get<PasswordValidationService>()
        )
    }

    viewModel {
        AccountStatusViewModel(
            accountStatusEventHandler = get<AccountStatusEventHandler>()
        )
    }

    factory { DeveloperInfoUiMapper() }

    viewModel {
        val getDeveloperInfoUseCase = get<GetDeveloperInfoUseCase>()
        val getAppLanguageUseCase = get<GetAppLanguageUseCase>()
        val developerInfoUiMapper = get<DeveloperInfoUiMapper>()
        DeveloperInfoViewModel(
            getDeveloperInfoUseCase = getDeveloperInfoUseCase,
            getAppLanguageUseCase = getAppLanguageUseCase,
            developerInfoUiMapper = developerInfoUiMapper
        )
    }

    factory<AccountSecurityUiMapper> { AccountSecurityUiMapperImpl() }

    factory<SubscriptionsUiMapper> { SubscriptionsUiMapperImpl(localeProvider = get()) }

    factory<PlanAllowancesUiMapper> {
        PlanAllowancesUiMapperImpl(localeProvider = get())
    }

    factory<LocalAiModelPackUiMapper> {
        LocalAiModelPackUiMapperImpl(localeProvider = get())
    }

    viewModel {
        val observeLocalAiModelPackStateUseCase = get<ObserveLocalAiModelPackStateUseCase>()
        val downloadLocalAiModelPackUseCase = get<DownloadLocalAiModelPackUseCase>()
        val cancelLocalAiModelPackDownloadUseCase = get<CancelLocalAiModelPackDownloadUseCase>()
        val deleteLocalAiModelPackUseCase = get<DeleteLocalAiModelPackUseCase>()
        val setLocalAiModelPackWifiOnlyUseCase = get<SetLocalAiModelPackWifiOnlyUseCase>()
        val localAiModelPackUiMapper = get<LocalAiModelPackUiMapper>()
        LocalAiModelPackViewModel(
            observeLocalAiModelPackStateUseCase = observeLocalAiModelPackStateUseCase,
            downloadLocalAiModelPackUseCase = downloadLocalAiModelPackUseCase,
            cancelLocalAiModelPackDownloadUseCase = cancelLocalAiModelPackDownloadUseCase,
            deleteLocalAiModelPackUseCase = deleteLocalAiModelPackUseCase,
            setLocalAiModelPackWifiOnlyUseCase = setLocalAiModelPackWifiOnlyUseCase,
            localAiModelPackUiMapper = localAiModelPackUiMapper
        )
    }

    viewModel {
        val observePlanAllowancesUseCase = get<ObservePlanAllowancesUseCase>()
        val getReceiptExtractionCapabilityUseCase = get<GetReceiptExtractionCapabilityUseCase>()
        val planAllowancesUiMapper = get<PlanAllowancesUiMapper>()
        PlanAllowancesViewModel(
            observePlanAllowancesUseCase = observePlanAllowancesUseCase,
            getReceiptExtractionCapabilityUseCase = getReceiptExtractionCapabilityUseCase,
            planAllowancesUiMapper = planAllowancesUiMapper
        )
    }

    viewModel {
        val getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>()
        val isUserAnonymousUseCase = get<IsUserAnonymousUseCase>()
        val getLinkedProvidersUseCase = get<GetLinkedProvidersUseCase>()
        val sendPasswordResetEmailUseCase = get<SendPasswordResetEmailUseCase>()
        val getBiometricCapabilityUseCase = get<GetBiometricCapabilityUseCase>()
        val getBiometricLockEnabledUseCase = get<GetBiometricLockEnabledUseCase>()
        val setBiometricLockEnabledUseCase = get<SetBiometricLockEnabledUseCase>()
        val accountSecurityUiMapper = get<AccountSecurityUiMapper>()
        AccountSecurityViewModel(
            getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
            isUserAnonymousUseCase = isUserAnonymousUseCase,
            getLinkedProvidersUseCase = getLinkedProvidersUseCase,
            sendPasswordResetEmailUseCase = sendPasswordResetEmailUseCase,
            getBiometricCapabilityUseCase = getBiometricCapabilityUseCase,
            getBiometricLockEnabledUseCase = getBiometricLockEnabledUseCase,
            setBiometricLockEnabledUseCase = setBiometricLockEnabledUseCase,
            accountSecurityUiMapper = accountSecurityUiMapper
        )
    }

    viewModel { params ->
        val entrySource = params.getOrNull<String>()
        val subscriptionsUseCases = SubscriptionsUseCases(
            getCurrentUserProfileUseCase = get<GetCurrentUserProfileUseCase>(),
            observeCurrentUserProfileUseCase = get<ObserveCurrentUserProfileUseCase>(),
            observeProAllowlistAccessUseCase = get<ObserveProAllowlistAccessUseCase>(),
            updateUserTierUseCase = get<UpdateUserTierUseCase>(),
            isUserAnonymousUseCase = get<IsUserAnonymousUseCase>(),
            reconcileSubscriptionEntitlementsUseCase = get<ReconcileSubscriptionEntitlementsUseCase>(),
            getReceiptExtractionCapabilityUseCase = get<GetReceiptExtractionCapabilityUseCase>()
        )
        val subscriptionsUiMapper = get<SubscriptionsUiMapper>()
        val billingService = get<BillingService>()
        val telemetryTracker = get<TelemetryTracker>()
        SubscriptionsViewModel(
            subscriptionsUseCases = subscriptionsUseCases,
            subscriptionsUiMapper = subscriptionsUiMapper,
            billingService = billingService,
            telemetryTracker = telemetryTracker,
            entrySource = entrySource
        )
    }

    single { DefaultCurrencyScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { LanguageScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { NotificationPreferencesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { SettingsScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { SubscriptionsScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { PlanAllowancesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { DeveloperServicesScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { ThemeScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { AccountStatusScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { AccountSecurityScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { FaqScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { PrivacyPolicyScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { OpenSourceScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { DeveloperInfoScreenUiProviderImpl() } bind ScreenUiProvider::class
    single { LocalAiModelPackScreenUiProviderImpl() } bind ScreenUiProvider::class
}
