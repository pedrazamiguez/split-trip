package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface ReconcileUnregisteredUserUseCase : UseCase {
    suspend operator fun invoke(email: String, activeUserId: String): Result<Unit>
}
