package es.pedrazamiguez.splittrip.domain.service.featuregate

import kotlinx.coroutines.flow.Flow

/**
 * Service that governs feature access and resource limits.
 * Decouples use cases and view models from concrete billing, tiering, or auth logic.
 */
interface FeatureGateService {

    /**
     * Checks if a specific premium or restricted feature is enabled.
     *
     * For collaborative capabilities (e.g. [GatedFeature.SUBUNIT_CREATION],
     * [GatedFeature.TRIP_REPORT_EXPORT]), access is enabled if either the acting authenticated
     * user or the group creator is on the Pro tier.
     * For [GatedFeature.GROUP_COVER_UPLOAD], access evaluates the acting user's shared monthly
     * media upload quota (`observeMediaUploadQuota().canUpload`).
     * For group-level creator-bound capabilities, if [groupId] is provided,
     * access is evaluated against the group creator's tier.
     * For user-level capabilities (e.g. AI receipt OCR scanning), access is evaluated
     * against the acting authenticated user's tier. For [GatedFeature.AI_RECEIPT_SCANNING],
     * access evaluates monthly scan consumption against the user's tier quota
     * (`aiReceiptMonthlyLimitFree` / `aiReceiptMonthlyLimitPro`).
     *
     * @param feature The feature to check.
     * @param groupId The optional ID of the group context, if evaluating a group-level capability.
     */
    fun isFeatureEnabled(feature: GatedFeature, groupId: String? = null): Flow<Boolean>

    /**
     * Observes the acting user's monthly AI receipt scanning quota for the current calendar month.
     */
    fun observeAiReceiptQuota(): Flow<AiReceiptQuota>

    /**
     * Observes the acting user's monthly manual receipt attachment quota for the current calendar month.
     */
    fun observeManualReceiptQuota(): Flow<ManualReceiptQuota>

    /**
     * Observes the acting user's shared monthly media upload quota (manual receipt attachments
     * and group cover photo uploads) for the current calendar month.
     */
    fun observeMediaUploadQuota(): Flow<MediaUploadQuota>

    /**
     * Observes a unified snapshot of the acting user's subscription tier, monthly AI receipt
     * scan quota, shared monthly media upload quota, owned-group usage, and tier-level allowances.
     */
    fun observePlanAllowances(): Flow<PlanAllowances>

    /**
     * Checks if the acting authenticated user is on the Pro subscription tier.
     */
    fun isActingUserPro(): Flow<Boolean>

    /**
     * Checks if the user is allowed to perform an action, considering current counts against limits.
     *
     * For collaborative limits (e.g. [GatedLimit.MAX_SUBUNITS_PER_GROUP]), collaborative Pro access
     * is preserved: if either the acting user or the group creator is Pro, the Pro limit applies.
     * For group-level limits (e.g. [GatedLimit.MAX_MEMBERS_PER_GROUP]), if [groupId] is provided,
     * the limit is evaluated against the group creator's tier.
     * For user-level limits (e.g. [GatedLimit.MAX_OWNED_GROUPS_COUNT]), the limit is evaluated
     * against the acting authenticated user's tier.
     *
     * @param limit The type of limit to check (e.g., maximum groups created).
     * @param currentCount The current number of resources created by this user.
     * @param groupId The optional ID of the group context, if evaluating a group-level limit.
     */
    fun checkLimit(limit: GatedLimit, currentCount: Int, groupId: String? = null): Flow<LimitResult>
}
