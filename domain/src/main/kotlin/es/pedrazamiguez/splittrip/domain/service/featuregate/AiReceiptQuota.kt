package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Snapshot of the acting user's monthly AI receipt scanning quota.
 *
 * @property usedScans Number of successful AI receipt extractions recorded in the current calendar month.
 * @property monthlyLimit Maximum allowed extractions for the acting user's effective tier in the current month.
 * @property isProTier True when the acting user has Pro entitlements (subscription or allowlist).
 * @property isGatingEnabled True when subscription gating is operationally enabled via Remote Config.
 */
data class AiReceiptQuota(
    val usedScans: Int,
    val monthlyLimit: Int?,
    val isProTier: Boolean,
    val isGatingEnabled: Boolean = true
) {
    /** True when the user has no monthly scan cap (subscription gating disabled or unlimited quota). */
    val isUnlimited: Boolean
        get() = !isGatingEnabled || monthlyLimit == null || monthlyLimit < 0

    /** Remaining monthly scans available, clamped at 0. Null when unlimited. */
    val remainingScans: Int?
        get() = if (isUnlimited) {
            null
        } else {
            val limit = monthlyLimit ?: 0
            (limit - usedScans).coerceAtLeast(0)
        }

    /** True when the user is allowed to perform an AI receipt scan right now. */
    val canScan: Boolean
        get() = isUnlimited || (remainingScans ?: 0) > 0

    /** True when gating is enabled, the user is on the Free tier, and a positive trial allocation is configured. */
    val isFreeTrialEligible: Boolean
        get() = isGatingEnabled && !isProTier && (monthlyLimit ?: 0) > 0

    /** True when gating is enabled, quota is exhausted, and upgrading to Pro lifts the restriction. */
    val requiresUpgrade: Boolean
        get() = isGatingEnabled && !canScan && !isProTier
}
