package es.pedrazamiguez.splittrip.domain.service.featuregate

import kotlinx.coroutines.flow.Flow

/**
 * Service that governs feature access and resource limits.
 * Decouples use cases and view models from concrete billing, tiering, or auth logic.
 */
interface FeatureGateService {

    /**
     * Checks if the active user has access to a specific premium or restricted feature.
     */
    fun isFeatureEnabled(feature: GatedFeature): Flow<Boolean>

    /**
     * Checks if the user is allowed to perform an action, considering current counts against limits.
     *
     * @param limit The type of limit to check (e.g., maximum groups created).
     * @param currentCount The current number of resources created by this user.
     */
    fun checkLimit(limit: GatedLimit, currentCount: Int): Flow<LimitResult>
}
