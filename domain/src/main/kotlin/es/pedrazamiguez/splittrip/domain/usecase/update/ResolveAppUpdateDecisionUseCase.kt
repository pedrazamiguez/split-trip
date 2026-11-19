package es.pedrazamiguez.splittrip.domain.usecase.update

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision

interface ResolveAppUpdateDecisionUseCase {
    suspend operator fun invoke(
        installedVersionCode: Long,
        availableVersionCode: Long
    ): AppUpdateDecision
}
