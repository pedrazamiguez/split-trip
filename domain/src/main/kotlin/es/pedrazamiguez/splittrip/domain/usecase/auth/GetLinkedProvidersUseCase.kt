package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetLinkedProvidersUseCase : UseCase {
    suspend operator fun invoke(): Result<List<AuthProviderType>>
}
