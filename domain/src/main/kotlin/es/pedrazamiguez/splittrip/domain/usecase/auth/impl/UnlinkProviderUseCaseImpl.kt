package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase

class UnlinkProviderUseCaseImpl(
    private val authenticationService: AuthenticationService
) : UnlinkProviderUseCase {

    override suspend operator fun invoke(providerType: AuthProviderType): Result<Unit> {
        return authenticationService.unlinkProvider(providerType)
    }
}
