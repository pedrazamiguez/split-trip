package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.createLoggingProxy
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInAnonymouslyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.GetLinkedProvidersUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.IsUserAnonymousUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkEmailPasswordUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkGoogleAccountUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SendPasswordResetEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInAnonymouslyUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInWithEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInWithGoogleUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignOutUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignUpWithEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.UnlinkProviderUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import org.koin.dsl.module

val authenticationDomainModule = module {
    factory<SignUpWithEmailUseCase> {
        createLoggingProxy<SignUpWithEmailUseCase>(
            SignUpWithEmailUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>(),
                userPreferenceRepository = get<UserPreferenceRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignInWithEmailUseCase> {
        createLoggingProxy<SignInWithEmailUseCase>(
            SignInWithEmailUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                userRepository = get<UserRepository>(),
                registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>(),
                userPreferenceRepository = get<UserPreferenceRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignInWithGoogleUseCase> {
        createLoggingProxy<SignInWithGoogleUseCase>(
            SignInWithGoogleUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>(),
                userPreferenceRepository = get<UserPreferenceRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignOutUseCase> {
        createLoggingProxy<SignOutUseCase>(
            SignOutUseCaseImpl(
                unregisterDeviceTokenUseCase = get<UnregisterDeviceTokenUseCase>(),
                localDatabaseCleaner = get<LocalDatabaseCleanerService>(),
                authenticationService = get<AuthenticationService>(),
                userPreferenceRepository = get<UserPreferenceRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SendPasswordResetEmailUseCase> {
        createLoggingProxy<SendPasswordResetEmailUseCase>(
            SendPasswordResetEmailUseCaseImpl(
                authService = get<AuthenticationService>(),
                emailValidationService = get<EmailValidationService>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<LinkGoogleAccountUseCase> {
        createLoggingProxy<LinkGoogleAccountUseCase>(
            LinkGoogleAccountUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                userRepository = get<UserRepository>(),
                reconcileUnregisteredUserUseCase = get<ReconcileUnregisteredUserUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<LinkEmailPasswordUseCase> {
        createLoggingProxy<LinkEmailPasswordUseCase>(
            LinkEmailPasswordUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                userRepository = get<UserRepository>(),
                reconcileUnregisteredUserUseCase = get<ReconcileUnregisteredUserUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<UnlinkProviderUseCase> {
        val authenticationService = get<AuthenticationService>()
        createLoggingProxy<UnlinkProviderUseCase>(
            UnlinkProviderUseCaseImpl(
                authenticationService = authenticationService
            ),
            LogTag.USE_CASE
        )
    }
    factory<GetLinkedProvidersUseCase> {
        val authenticationService = get<AuthenticationService>()
        createLoggingProxy<GetLinkedProvidersUseCase>(
            GetLinkedProvidersUseCaseImpl(
                authenticationService = authenticationService
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignInAnonymouslyUseCase> {
        createLoggingProxy<SignInAnonymouslyUseCase>(
            SignInAnonymouslyUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                userPreferenceRepository = get<UserPreferenceRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<IsUserAnonymousUseCase> {
        createLoggingProxy<IsUserAnonymousUseCase>(
            IsUserAnonymousUseCaseImpl(
                authenticationService = get<AuthenticationService>()
            ),
            LogTag.USE_CASE
        )
    }
}
