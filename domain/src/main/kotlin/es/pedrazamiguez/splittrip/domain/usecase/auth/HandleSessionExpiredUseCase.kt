package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface HandleSessionExpiredUseCase : UseCase {
    suspend operator fun invoke(): Result<Unit>
}
