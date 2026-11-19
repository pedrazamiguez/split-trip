package es.pedrazamiguez.splittrip.navigation

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.user.CheckPendingReconciliationUseCase

internal data class StartupReconciliationDependencies(
    val authenticationService: AuthenticationService,
    val userPreferenceRepository: UserPreferenceRepository,
    val checkPendingReconciliationUseCase: CheckPendingReconciliationUseCase
)
