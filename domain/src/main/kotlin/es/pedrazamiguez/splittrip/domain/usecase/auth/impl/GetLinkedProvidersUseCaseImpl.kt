package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase

class GetLinkedProvidersUseCaseImpl(
    private val authenticationService: AuthenticationService
) : GetLinkedProvidersUseCase {

    override suspend operator fun invoke(): Result<List<AuthProviderType>> {
        return authenticationService.getLinkedProviders()
    }
}
