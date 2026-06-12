package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SendPasswordResetEmailUseCase : UseCase {
    suspend operator fun invoke(email: String): Result<Unit>
}
