package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface LinkGoogleAccountUseCase : UseCase {
    suspend operator fun invoke(idToken: String): Result<Unit>
}
