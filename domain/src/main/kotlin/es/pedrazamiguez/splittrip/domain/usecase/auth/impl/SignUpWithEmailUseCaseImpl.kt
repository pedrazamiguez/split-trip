package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase

class SignUpWithEmailUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
) : SignUpWithEmailUseCase {

    override suspend operator fun invoke(
        email: String,
        displayName: String,
        password: String
    ): Result<String> = runCatching {
        val userId = authenticationService.signUp(email, displayName, password).getOrThrow()
        registerDeviceTokenUseCase().onFailure {
            // Device token registration is best-effort and should not
            // cause the email sign-up flow to fail.
        }
        userId
    }
}
