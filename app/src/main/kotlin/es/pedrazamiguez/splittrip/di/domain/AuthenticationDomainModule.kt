package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.createLoggingProxy
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInWithEmailUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignInWithGoogleUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.SignOutUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import org.koin.dsl.module

val authenticationDomainModule = module {
    factory<SignInWithEmailUseCase> {
        createLoggingProxy<SignInWithEmailUseCase>(
            SignInWithEmailUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                userRepository = get<UserRepository>(),
                registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignInWithGoogleUseCase> {
        createLoggingProxy<SignInWithGoogleUseCase>(
            SignInWithGoogleUseCaseImpl(
                authenticationService = get<AuthenticationService>(),
                registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<SignOutUseCase> {
        createLoggingProxy<SignOutUseCase>(
            SignOutUseCaseImpl(
                unregisterDeviceTokenUseCase = get<UnregisterDeviceTokenUseCase>(),
                localDatabaseCleaner = get<LocalDatabaseCleanerService>(),
                authenticationService = get<AuthenticationService>()
            ),
            LogTag.USE_CASE
        )
    }
}
