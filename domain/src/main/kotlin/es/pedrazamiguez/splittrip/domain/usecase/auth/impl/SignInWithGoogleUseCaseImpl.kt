package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase

class SignInWithGoogleUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
) : SignInWithGoogleUseCase {

    override suspend operator fun invoke(idToken: String): Result<String> = runCatching {
        val user = authenticationService
            .signInWithGoogle(idToken)
            .getOrThrow()

        registerDeviceTokenUseCase()
            .onFailure {
                // Device token registration is best-effort and should not
                // cause the Google sign-in flow to fail.
            }

        userPreferenceRepository.setHasSignedOut(false)
        reconcileUnregisteredUserUseCase(user.email, user.userId).getOrThrow()

        user.userId
    }
}
