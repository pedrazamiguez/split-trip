package es.pedrazamiguez.splittrip.data.repository.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LiteRtRuntimeConfigTest {

    @Test
    fun `buildEngineSpec uses CPU backend, bounded maxNumTokens, modelPath, and cacheDir`() {
        val modelPath = "/data/models/gemma4.litertlm"
        val cacheDir = "/cache"

        val spec = LiteRtRuntimeConfig.buildEngineSpec(
            modelPath = modelPath,
            cacheDir = cacheDir
        )

        assertEquals(LiteRtRuntimeConfig.BackendType.CPU, spec.backend)
        assertEquals(LiteRtRuntimeConfig.DEFAULT_MAX_NUM_TOKENS, spec.maxNumTokens)
        assertEquals(modelPath, spec.modelPath)
        assertEquals(cacheDir, spec.cacheDir)
    }

    @Test
    fun `buildConversationSpec disables thinking tokens, bounds maxOutputTokens, and applies deterministic sampler`() {
        val spec = LiteRtRuntimeConfig.buildConversationSpec(
            systemInstruction = "Return JSON",
            maxOutputTokens = LiteRtRuntimeConfig.DEFAULT_MAX_OUTPUT_TOKENS
        )

        assertEquals(
            LiteRtRuntimeConfig.ThinkingSpec(enableThinking = false, thinkingTokenBudget = 0),
            spec.thinkingSpec
        )
        assertEquals(384, spec.maxOutputTokens)
        assertEquals("Return JSON", spec.systemInstruction)
        assertEquals(LiteRtRuntimeConfig.DEFAULT_SAMPLER_SPEC, spec.samplerSpec)
    }

    @Test
    fun `buildConversationSpec allows null systemInstruction while keeping thinking disabled`() {
        val spec = LiteRtRuntimeConfig.buildConversationSpec(systemInstruction = null)

        assertNull(spec.systemInstruction)
        assertEquals(false, spec.thinkingSpec.enableThinking)
        assertEquals(0, spec.thinkingSpec.thinkingTokenBudget)
    }
}
