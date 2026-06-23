package es.pedrazamiguez.splittrip.features.onboarding.di

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.ReconciliationViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingUiModule = module {
    viewModel {
        val reconcileUnregisteredUserUseCase = get<ReconcileUnregisteredUserUseCase>()
        val authenticationService = get<AuthenticationService>()
        ReconciliationViewModel(
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase,
            authenticationService = authenticationService
        )
    }
}
