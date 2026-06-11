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
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authenticationUiModule = module {
    viewModel {
        val signInWithEmailUseCase = get<SignInWithEmailUseCase>()
        val signInWithGoogleUseCase = get<SignInWithGoogleUseCase>()
        val linkGoogleAccountUseCase = get<LinkGoogleAccountUseCase>()
        AuthenticationViewModel(
            signInWithEmailUseCase = signInWithEmailUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
            linkGoogleAccountUseCase = linkGoogleAccountUseCase
        )
    }
    viewModel {
        RegisterViewModel(
            signUpWithEmailUseCase = get<SignUpWithEmailUseCase>(),
            emailValidationService = get<EmailValidationService>()
        )
    }
    viewModel {
        ForgotPasswordViewModel(
            sendPasswordResetEmailUseCase = get<SendPasswordResetEmailUseCase>()
        )
    }
}
