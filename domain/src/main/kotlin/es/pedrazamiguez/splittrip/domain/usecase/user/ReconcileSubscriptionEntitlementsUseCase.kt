package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface ReconcileSubscriptionEntitlementsUseCase : UseCase {
    suspend operator fun invoke(): Result<Unit>
}
