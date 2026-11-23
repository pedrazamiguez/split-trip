package es.pedrazamiguez.splittrip.domain.service.featuregate

/**
 * Snapshot of the acting user's monthly group report export quota.
 *
 * Free users receive a monthly summary PDF allowance (excluding itemised breakdowns and CSV),
 * while Pro users (or ungated sessions) receive unlimited full PDF and CSV exports.
 */
data class ReportExportQuota(
    val usedExports: Int,
    val monthlyFreeLimit: Int,
    val isProTier: Boolean,
    val isGatingEnabled: Boolean = true
) {
    /** True when the user has no monthly export cap (Pro tier or subscription gating disabled). */
    val isUnlimited: Boolean
        get() = !isGatingEnabled || isProTier

    /** Effective monthly export limit, or null when unlimited. */
    val monthlyLimit: Int?
        get() = if (isUnlimited) null else monthlyFreeLimit

    /** Remaining monthly summary PDF exports for Free users, clamped at 0. Null when Pro or ungated. */
    val remainingExports: Int?
        get() = if (isUnlimited) {
            null
        } else {
            (monthlyFreeLimit - usedExports).coerceAtLeast(0)
        }

    /** True when the user can export a PDF report right now. */
    val canExportPdf: Boolean
        get() = isUnlimited || (monthlyFreeLimit - usedExports) > 0

    /** True when the user can export a machine-readable CSV spreadsheet (Pro-only when gated). */
    val canExportCsv: Boolean
        get() = isUnlimited

    /** True when PDF exports include full itemised expenses, contributions, and cash withdrawals. */
    val includesItemisedDetails: Boolean
        get() = isUnlimited

    /** True when gating is enabled, the user is on the Free tier, and a positive Free allowance is configured. */
    val isFreeQuotaTracked: Boolean
        get() = isGatingEnabled && !isProTier && monthlyFreeLimit > 0

    /** True when gating is enabled, the user is on the Free tier, and the monthly PDF quota is exhausted. */
    val requiresUpgrade: Boolean
        get() = isGatingEnabled && !isProTier && !canExportPdf
}
