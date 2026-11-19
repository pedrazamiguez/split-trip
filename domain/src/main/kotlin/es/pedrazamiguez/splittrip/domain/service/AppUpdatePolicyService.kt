package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.AppUpdatePolicy
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal

interface AppUpdatePolicyService {
    fun resolveDecision(
        installedVersionCode: Long,
        availableVersionCode: Long,
        policy: AppUpdatePolicy,
        lastDismissal: OptionalUpdateDismissal?,
        nowTimestampMs: Long
    ): AppUpdateDecision
}
