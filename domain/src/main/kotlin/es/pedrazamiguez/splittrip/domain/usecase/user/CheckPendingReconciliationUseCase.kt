package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface CheckPendingReconciliationUseCase : UseCase {
    suspend operator fun invoke(email: String): Result<Boolean>
}
