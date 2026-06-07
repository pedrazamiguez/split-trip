package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SignInWithGoogleUseCase : UseCase {
    suspend operator fun invoke(idToken: String): Result<String>
}
