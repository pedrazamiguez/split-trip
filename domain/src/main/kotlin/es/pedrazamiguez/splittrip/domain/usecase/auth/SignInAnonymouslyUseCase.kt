package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SignInAnonymouslyUseCase : UseCase {
    suspend operator fun invoke(): Result<String>
}
