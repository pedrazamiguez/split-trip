package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.flow.Flow

interface AiModelResolverService {
    /**
     * Returns the active model that should be used for receipt extraction.
     * In automatic mode (no developer override), prefers [AiEngineType.LITE_RT_LM]
     * when the optional local AI model pack is installed and the user is entitled,
     * falling back cleanly to [AiEngineType.AI_CORE_GEMMA_4].
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

    /**
     * Returns true if Android AICore (Gemini Nano) is supported on this device.
     */
    fun isAiCoreSupported(): Boolean
}
