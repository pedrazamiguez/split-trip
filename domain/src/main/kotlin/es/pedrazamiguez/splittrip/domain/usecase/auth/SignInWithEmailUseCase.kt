package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SignInWithEmailUseCase : UseCase {
    suspend operator fun invoke(email: String, password: String): Result<String>
}
