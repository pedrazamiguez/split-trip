package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase

class SignUpWithEmailUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
) : SignUpWithEmailUseCase {

    override suspend operator fun invoke(
        email: String,
        displayName: String,
        password: String
    ): Result<String> = runCatching {
        val normalizedEmail = User.normalizeEmail(email)
        val userId = authenticationService.signUp(normalizedEmail, displayName, password).getOrThrow()
        registerDeviceTokenUseCase().onFailure {
            // Device token registration is best-effort and should not
            // cause the email sign-up flow to fail.
        }
        userPreferenceRepository.setHasSignedOut(false)
        reconcileUnregisteredUserUseCase(normalizedEmail, userId).getOrThrow()
        userId
    }
}
