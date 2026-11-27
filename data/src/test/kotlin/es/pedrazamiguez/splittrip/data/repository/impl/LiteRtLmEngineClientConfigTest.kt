package es.pedrazamiguez.splittrip.data.repository.impl

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ThinkingConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiteRtLmEngineClientConfigTest {

    @Test
    fun `buildEngineConfig uses CPU backend, bounded maxNumTokens, modelPath, and cacheDir`() {
        val modelPath = "/data/models/gemma4.litertlm"
        val cacheDir = "/cache"

        val config = LiteRtLmEngineClientImpl.buildEngineConfig(
            modelPath = modelPath,
            cacheDir = cacheDir
        )

        assertTrue(config.backend is Backend.CPU)
        assertEquals(LiteRtLmEngineClientImpl.DEFAULT_MAX_NUM_TOKENS, config.maxNumTokens)
        assertEquals(modelPath, config.modelPath)
        assertEquals(cacheDir, config.cacheDir)
    }

    @Test
    fun `buildConversationConfig disables thinking tokens and bounds maxOutputToken`() {
        val config = LiteRtLmEngineClientImpl.buildConversationConfig(
            systemInstruction = "Return JSON",
            maxOutputTokens = LiteRtLmEngineClientImpl.DEFAULT_MAX_OUTPUT_TOKENS
        )

        assertEquals(
            ThinkingConfig(enableThinking = false, thinkingTokenBudget = 0),
            config.thinkingConfig
        )
        assertEquals(384, config.maxOutputToken)
        assertNotNull(config.systemInstruction)
        assertNotNull(config.samplerConfig)
    }

    @Test
    fun `buildConversationConfig allows null systemInstruction while keeping thinking disabled`() {
        val config = LiteRtLmEngineClientImpl.buildConversationConfig(systemInstruction = null)

        assertNull(config.systemInstruction)
        assertEquals(false, config.thinkingConfig?.enableThinking)
        assertEquals(0, config.thinkingConfig?.thinkingTokenBudget)
    }
}
