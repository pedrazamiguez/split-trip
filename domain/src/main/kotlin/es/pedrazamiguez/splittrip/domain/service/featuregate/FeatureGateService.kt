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
     * For collaborative capabilities (e.g. [GatedFeature.SUBUNIT_CREATION]), access is enabled
     * if either the acting authenticated user or the group creator is on the Pro tier.
     * For group-level creator-bound capabilities (e.g. cover photo uploads), if [groupId] is provided,
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
     * Checks if the acting authenticated user is on the Pro subscription tier.
     */
    fun isActingUserPro(): Flow<Boolean>

    /**
     * Checks if the user is allowed to perform an action, considering current counts against limits.
     *
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
