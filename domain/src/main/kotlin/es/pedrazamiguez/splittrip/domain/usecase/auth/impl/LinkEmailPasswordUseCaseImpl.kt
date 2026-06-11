package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase

class LinkEmailPasswordUseCaseImpl(
    private val authenticationService: AuthenticationService
) : LinkEmailPasswordUseCase {

    override suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authenticationService.linkEmailPassword(email, password)
    }
}
