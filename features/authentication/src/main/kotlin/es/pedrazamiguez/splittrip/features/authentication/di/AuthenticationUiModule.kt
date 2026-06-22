package es.pedrazamiguez.splittrip.features.authentication.di

import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInAnonymouslyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.AuthenticationViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.ForgotPasswordViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.RegisterViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.AuthenticationCollisionEventHandler
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.AuthenticationCollisionEventHandlerImpl
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.RegisterSubmitEventHandler
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.RegisterSubmitEventHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authenticationUiModule = module {
    factory<AuthenticationCollisionEventHandler> {
        val signInWithEmailUseCase = get<SignInWithEmailUseCase>()
        val linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>()
        AuthenticationCollisionEventHandlerImpl(
            signInWithEmailUseCase = signInWithEmailUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase
        )
    }

    factory<RegisterSubmitEventHandler> {
        val signUpWithEmailUseCase = get<SignUpWithEmailUseCase>()
        val emailValidationService = get<EmailValidationService>()
        RegisterSubmitEventHandlerImpl(
            signUpWithEmailUseCase = signUpWithEmailUseCase,
            emailValidationService = emailValidationService
        )
    }

    viewModel {
        val signInWithEmailUseCase = get<SignInWithEmailUseCase>()
        val signInWithGoogleUseCase = get<SignInWithGoogleUseCase>()
        val signInAnonymouslyUseCase = get<SignInAnonymouslyUseCase>()
        val authenticationCollisionEventHandler = get<AuthenticationCollisionEventHandler>()
        AuthenticationViewModel(
            signInWithEmailUseCase = signInWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
            signInAnonymouslyUseCase = signInAnonymouslyUseCase,
            authenticationCollisionEventHandler = authenticationCollisionEventHandler
        )
    }

    viewModel {
        val registerSubmitEventHandler = get<RegisterSubmitEventHandler>()
        RegisterViewModel(
            registerSubmitEventHandler = registerSubmitEventHandler
        )
    }

    viewModel {
        ForgotPasswordViewModel(
            sendPasswordResetEmailUseCase = get<SendPasswordResetEmailUseCase>()
        )
    }
}
