package es.pedrazamiguez.splittrip.data.litert

object LiteRtRuntimeConfig {

    const val DEFAULT_MAX_NUM_TOKENS = 2_560
    const val DEFAULT_MAX_OUTPUT_TOKENS = 384
    const val DEFAULT_TOP_K = 40
    const val DEFAULT_TOP_P = 0.95
    const val DEFAULT_TEMPERATURE = 0.2
    const val DEFAULT_SEED = 0

    enum class BackendType {
        CPU
    }

    data class ThinkingSpec(
        val enableThinking: Boolean,
        val thinkingTokenBudget: Int
    )

    data class SamplerSpec(
        val topK: Int,
        val topP: Double,
        val temperature: Double,
        val seed: Int
    )

    data class EngineSpec(
        val modelPath: String,
        val backend: BackendType,
        val maxNumTokens: Int,
        val cacheDir: String
    )

    data class ConversationSpec(
        val systemInstruction: String?,
        val samplerSpec: SamplerSpec,
        val maxOutputTokens: Int,
        val thinkingSpec: ThinkingSpec
    )

    val DISABLED_THINKING_SPEC = ThinkingSpec(
        enableThinking = false,
        thinkingTokenBudget = 0
    )

    val DEFAULT_SAMPLER_SPEC = SamplerSpec(
        topK = DEFAULT_TOP_K,
        topP = DEFAULT_TOP_P,
        temperature = DEFAULT_TEMPERATURE,
        seed = DEFAULT_SEED
    )

    fun buildEngineSpec(
        modelPath: String,
        cacheDir: String
    ): EngineSpec = EngineSpec(
        modelPath = modelPath,
        backend = BackendType.CPU,
        maxNumTokens = DEFAULT_MAX_NUM_TOKENS,
        cacheDir = cacheDir
    )

    fun buildConversationSpec(
        systemInstruction: String?,
        maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS
    ): ConversationSpec = ConversationSpec(
        systemInstruction = systemInstruction,
        samplerSpec = DEFAULT_SAMPLER_SPEC,
        maxOutputTokens = maxOutputTokens,
        thinkingSpec = DISABLED_THINKING_SPEC
    )
}
