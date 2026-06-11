package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase

class LinkGoogleAccountUseCaseImpl(
    private val authenticationService: AuthenticationService
) : LinkGoogleAccountUseCase {

    override suspend operator fun invoke(idToken: String): Result<Unit> {
        return authenticationService.linkGoogleAccount(idToken)
    }
}
