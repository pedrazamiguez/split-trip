package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.flow.Flow

interface AiModelResolverService {
    /**
     * Returns the active model that should be used for receipt extraction.
     * This flow emits the resolved engine type based on capabilities, active
     * overrides, or user subscription tiers.
     */
    fun getActiveModel(): Flow<AiEngineType>

    /**
     * Gets the developer override model if configured.
     */
    fun getDeveloperOverrideModel(): Flow<AiEngineType?>

    /**
     * Sets the developer override model. Pass null to reset to automatic.
     */
    suspend fun setDeveloperOverrideModel(model: AiEngineType?)
}
