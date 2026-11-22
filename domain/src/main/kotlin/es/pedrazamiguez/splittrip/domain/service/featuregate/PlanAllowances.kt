package es.pedrazamiguez.splittrip.domain.service.featuregate

import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier

/**
 * Reactive snapshot of the acting user's current plan tier, live quota usage,
 * and tier-level feature allowances.
 *
 * @property maxSubunitsPerGroup Maximum subunits allowed per group for the user's tier,
 *   or `null` when unlimited (Pro tier or subscription gating disabled).
 */
data class PlanAllowances(
    val tier: SubscriptionTier,
    val isGatingEnabled: Boolean,
    val aiReceiptQuota: AiReceiptQuota,
    val mediaUploadQuota: MediaUploadQuota,
    val ownedGroupsUsed: Int,
    val maxOwnedGroups: Int,
    val maxMembersPerGroup: Int,
    val maxSubunitsPerGroup: Int?,
    val isAdFreeEnabled: Boolean
) {
    val isProTier: Boolean
        get() = tier == SubscriptionTier.PRO

    val ownedGroupsRemaining: Int
        get() = (maxOwnedGroups - ownedGroupsUsed).coerceAtLeast(0)

    val showUpgradeCta: Boolean
        get() = isGatingEnabled && !isProTier
}
