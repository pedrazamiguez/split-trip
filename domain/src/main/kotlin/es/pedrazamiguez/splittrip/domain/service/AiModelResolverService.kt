package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import kotlinx.coroutines.flow.Flow

interface AiModelResolverService {
    /**
     * Returns the active model that should be used for receipt extraction and AI classification.
     * In automatic mode (no developer override), prefers [AiEngineType.AI_CORE_GEMMA_4]
     * whenever AICore is supported on the device, using [AiEngineType.LITE_RT_LM] when
     * AICore is unsupported and the optional local AI model pack is installed and entitled.
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
