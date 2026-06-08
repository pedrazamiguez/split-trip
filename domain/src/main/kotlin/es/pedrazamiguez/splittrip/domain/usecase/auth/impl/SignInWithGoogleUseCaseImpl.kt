package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase

class SignInWithGoogleUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
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

        user.userId
    }
}
