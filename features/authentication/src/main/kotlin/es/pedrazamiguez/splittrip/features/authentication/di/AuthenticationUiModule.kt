package es.pedrazamiguez.splittrip.features.authentication.di

import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.AuthenticationViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.ForgotPasswordViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.RegisterViewModel
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.AuthenticationCollisionHandler
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.AuthenticationCollisionHandlerImpl
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.RegisterSubmitHandler
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.RegisterSubmitHandlerImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authenticationUiModule = module {
    factory<AuthenticationCollisionHandler> {
        val signInWithEmailUseCase = get<SignInWithEmailUseCase>()
        val linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>()
        AuthenticationCollisionHandlerImpl(
            signInWithEmailUseCase = signInWithEmailUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase
        )
    }

    factory<RegisterSubmitHandler> {
        val signUpWithEmailUseCase = get<SignUpWithEmailUseCase>()
        val emailValidationService = get<EmailValidationService>()
        RegisterSubmitHandlerImpl(
            signUpWithEmailUseCase = signUpWithEmailUseCase,
            emailValidationService = emailValidationService
        )
    }

    viewModel {
        val signInWithEmailUseCase = get<SignInWithEmailUseCase>()
        val signInWithGoogleUseCase = get<SignInWithGoogleUseCase>()
        val authenticationCollisionHandler = get<AuthenticationCollisionHandler>()
        AuthenticationViewModel(
            signInWithEmailUseCase = signInWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
            authenticationCollisionHandler = authenticationCollisionHandler
        )
    }

    viewModel {
        val registerSubmitHandler = get<RegisterSubmitHandler>()
        RegisterViewModel(
            registerSubmitHandler = registerSubmitHandler
        )
    }

    viewModel {
        ForgotPasswordViewModel(
            sendPasswordResetEmailUseCase = get<SendPasswordResetEmailUseCase>()
        )
    }
}
