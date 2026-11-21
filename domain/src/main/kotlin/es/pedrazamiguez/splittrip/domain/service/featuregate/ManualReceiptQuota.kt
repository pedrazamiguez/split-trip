package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Snapshot of the acting user's monthly manual receipt attachment quota.
 */
data class ManualReceiptQuota(
    val usedAttachments: Int,
    val monthlyFreeLimit: Int,
    val isProTier: Boolean,
    val isGatingEnabled: Boolean = true
) {
    /** Remaining monthly manual receipt attachments for Free users, clamped at 0. Null when Pro or ungated (unlimited). */
    val remainingAttachments: Int?
        get() = if (!isGatingEnabled || isProTier) {
            null
        } else {
            (monthlyFreeLimit - usedAttachments).coerceAtLeast(0)
        }

    /** True when the user is allowed to attach a new manual receipt right now. */
    val canAttach: Boolean
        get() = !isGatingEnabled || isProTier || (monthlyFreeLimit - usedAttachments) > 0

    /** True when gating is enabled, the user is on the Free tier, and a positive Free allowance is configured. */
    val isFreeQuotaTracked: Boolean
        get() = isGatingEnabled && !isProTier && monthlyFreeLimit > 0

    /** True when gating is enabled, the user is on the Free tier, and the monthly quota is exhausted. */
    val requiresUpgrade: Boolean
        get() = isGatingEnabled && !isProTier && !canAttach
}
