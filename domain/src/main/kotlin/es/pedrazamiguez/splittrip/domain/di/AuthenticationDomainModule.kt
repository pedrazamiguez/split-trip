package es.pedrazamiguez.splittrip.domain.di

import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignOutUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import org.koin.dsl.module

val authenticationDomainModule = module {
    factory {
        SignInWithEmailUseCase(
            authenticationService = get<AuthenticationService>(),
            userRepository = get<UserRepository>(),
            registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>()
        )
    }
    factory {
        SignInWithGoogleUseCase(
            authenticationService = get<AuthenticationService>(),
            registerDeviceTokenUseCase = get<RegisterDeviceTokenUseCase>()
        )
    }
    factory {
        SignOutUseCase(
            unregisterDeviceTokenUseCase = get<UnregisterDeviceTokenUseCase>(),
            localDatabaseCleaner = get<LocalDatabaseCleanerService>(),
            authenticationService = get<AuthenticationService>()
        )
    }
}
