package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Snapshot of the acting user's shared monthly media upload quota
 * (covering manual receipt attachments and group cover photo uploads).
 */
data class MediaUploadQuota(
    val usedUploads: Int,
    val monthlyFreeLimit: Int,
    val isProTier: Boolean,
    val isGatingEnabled: Boolean = true
) {
    /** True when the user has no monthly upload cap (Pro tier or subscription gating disabled). */
    val isUnlimited: Boolean
        get() = !isGatingEnabled || isProTier

    /** Effective monthly upload limit, or null when unlimited. */
    val monthlyLimit: Int?
        get() = if (isUnlimited) null else monthlyFreeLimit

    /** Remaining monthly media uploads for Free users, clamped at 0. Null when Pro or ungated. */
    val remainingUploads: Int?
        get() = if (isUnlimited) {
            null
        } else {
            (monthlyFreeLimit - usedUploads).coerceAtLeast(0)
        }

    /** True when the user is allowed to perform a new media upload right now. */
    val canUpload: Boolean
        get() = isUnlimited || (monthlyFreeLimit - usedUploads) > 0

    /** True when gating is enabled, the user is on the Free tier, and a positive Free allowance is configured. */
    val isFreeQuotaTracked: Boolean
        get() = isGatingEnabled && !isProTier && monthlyFreeLimit > 0

    /** True when gating is enabled, the user is on the Free tier, and the monthly quota is exhausted. */
    val requiresUpgrade: Boolean
        get() = isGatingEnabled && !isProTier && !canUpload
}
