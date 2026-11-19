package es.pedrazamiguez.splittrip.domain.usecase.update.impl

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.AppUpdatePolicy
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AppUpdatePolicyService
import es.pedrazamiguez.splittrip.domain.usecase.update.ResolveAppUpdateDecisionUseCase

class ResolveAppUpdateDecisionUseCaseImpl(
    private val appConfigService: AppConfigService,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val appUpdatePolicyService: AppUpdatePolicyService,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : ResolveAppUpdateDecisionUseCase {

    override suspend fun invoke(
        installedVersionCode: Long,
        availableVersionCode: Long
    ): AppUpdateDecision {
        val policy = AppUpdatePolicy(
            inAppUpdatesEnabled = appConfigService.inAppUpdatesEnabled.value,
            minSupportedVersionCode = appConfigService.minSupportedVersionCode.value,
            autoForcePatchUpdates = appConfigService.autoForcePatchUpdates.value,
            optionalUpdateVersionCodes = appConfigService.optionalUpdateVersionCodes.value,
            optionalUpdateCooldownHours = appConfigService.optionalUpdateCooldownHours.value
        )
        val lastDismissal = userPreferenceRepository.getOptionalUpdateDismissal()
        return appUpdatePolicyService.resolveDecision(
            installedVersionCode = installedVersionCode,
            availableVersionCode = availableVersionCode,
            policy = policy,
            lastDismissal = lastDismissal,
            nowTimestampMs = currentTimeMillis()
        )
    }
}
