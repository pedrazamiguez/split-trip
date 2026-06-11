package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UnlinkProviderUseCase : UseCase {
    suspend operator fun invoke(providerType: AuthProviderType): Result<Unit>
}
