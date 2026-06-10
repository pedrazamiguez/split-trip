package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SignUpWithEmailUseCase : UseCase {
    suspend operator fun invoke(email: String, displayName: String, password: String): Result<String>
}
