package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.AppUpdateDecision
import es.pedrazamiguez.splittrip.domain.model.AppUpdatePolicy
import es.pedrazamiguez.splittrip.domain.model.ForcedUpdateReason
import es.pedrazamiguez.splittrip.domain.model.OptionalUpdateDismissal
import es.pedrazamiguez.splittrip.domain.model.SemanticVersion
import es.pedrazamiguez.splittrip.domain.service.AppUpdatePolicyService

class AppUpdatePolicyServiceImpl : AppUpdatePolicyService {

    override fun resolveDecision(
        installedVersionCode: Long,
        availableVersionCode: Long,
        policy: AppUpdatePolicy,
        lastDismissal: OptionalUpdateDismissal?,
        nowTimestampMs: Long
    ): AppUpdateDecision {
        if (!policy.inAppUpdatesEnabled) return AppUpdateDecision.None
        if (installedVersionCode < 0 || availableVersionCode <= installedVersionCode) {
            return AppUpdateDecision.None
        }

        val installedVersion = SemanticVersion.fromVersionCode(installedVersionCode)
            ?: return AppUpdateDecision.None
        val availableVersion = SemanticVersion.fromVersionCode(availableVersionCode)
            ?: return AppUpdateDecision.None

        return resolveVersionDecision(
            installedVersionCode = installedVersionCode,
            availableVersionCode = availableVersionCode,
            installedVersion = installedVersion,
            availableVersion = availableVersion,
            policy = policy,
            lastDismissal = lastDismissal,
            nowTimestampMs = nowTimestampMs
        )
    }

    private fun resolveVersionDecision(
        installedVersionCode: Long,
        availableVersionCode: Long,
        installedVersion: SemanticVersion,
        availableVersion: SemanticVersion,
        policy: AppUpdatePolicy,
        lastDismissal: OptionalUpdateDismissal?,
        nowTimestampMs: Long
    ): AppUpdateDecision {
        val forcedReason = resolveForcedReason(
            installedVersionCode = installedVersionCode,
            availableVersionCode = availableVersionCode,
            installedVersion = installedVersion,
            availableVersion = availableVersion,
            policy = policy
        )
        return if (forcedReason != null) {
            AppUpdateDecision.Forced(
                availableVersionCode = availableVersionCode,
                availableVersion = availableVersion,
                reason = forcedReason
            )
        } else {
            resolveOptionalWithCooldown(
                availableVersionCode = availableVersionCode,
                availableVersion = availableVersion,
                cooldownHours = policy.optionalUpdateCooldownHours,
                lastDismissal = lastDismissal,
                nowTimestampMs = nowTimestampMs
            )
        }
    }

    private fun resolveForcedReason(
        installedVersionCode: Long,
        availableVersionCode: Long,
        installedVersion: SemanticVersion,
        availableVersion: SemanticVersion,
        policy: AppUpdatePolicy
    ): ForcedUpdateReason? = when {
        policy.minSupportedVersionCode > 0 && installedVersionCode < policy.minSupportedVersionCode ->
            ForcedUpdateReason.BELOW_MIN_SUPPORTED_VERSION
        availableVersionCode in policy.optionalUpdateVersionCodes -> null
        availableVersion.major > installedVersion.major ->
            ForcedUpdateReason.MAJOR_VERSION_UPDATE
        policy.autoForcePatchUpdates && availableVersion.patch > 0 ->
            ForcedUpdateReason.PATCH_HOTFIX_UPDATE
        else -> null
    }

    private fun resolveOptionalWithCooldown(
        availableVersionCode: Long,
        availableVersion: SemanticVersion,
        cooldownHours: Long,
        lastDismissal: OptionalUpdateDismissal?,
        nowTimestampMs: Long
    ): AppUpdateDecision = if (
        isWithinCooldown(
            availableVersionCode = availableVersionCode,
            cooldownHours = cooldownHours,
            lastDismissal = lastDismissal,
            nowTimestampMs = nowTimestampMs
        )
    ) {
        AppUpdateDecision.None
    } else {
        AppUpdateDecision.Optional(
            availableVersionCode = availableVersionCode,
            availableVersion = availableVersion
        )
    }

    private fun isWithinCooldown(
        availableVersionCode: Long,
        cooldownHours: Long,
        lastDismissal: OptionalUpdateDismissal?,
        nowTimestampMs: Long
    ): Boolean {
        if (lastDismissal == null || cooldownHours <= 0 || lastDismissal.versionCode != availableVersionCode) {
            return false
        }
        val elapsedMs = nowTimestampMs - lastDismissal.dismissedAtTimestampMs
        if (elapsedMs < 0) return false
        return elapsedMs < cooldownHours * MILLIS_PER_HOUR
    }

    private companion object {
        const val MILLIS_PER_HOUR: Long = 3_600_000L
    }
}
