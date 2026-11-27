package es.pedrazamiguez.splittrip.features.settings.presentation.extensions

import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AiEngineDisplayExtensions")
class AiEngineDisplayExtensionsTest {

    @Test
    fun `toLabelRes maps all ModelPackTier entries to expected string resources`() {
        assertEquals(R.string.local_ai_pack_tier_compact, ModelPackTier.COMPACT.toLabelRes())
        assertEquals(R.string.local_ai_pack_tier_balanced, ModelPackTier.BALANCED.toLabelRes())
        assertEquals(R.string.local_ai_pack_tier_high_quality, ModelPackTier.HIGH_QUALITY.toLabelRes())
    }

    @Test
    fun `formatLiteRtDisplayLabel returns baseLabel when installedPack is null`() {
        val result = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = null,
            localizedTierLabel = "Balanced"
        )

        assertEquals("LiteRT-LM", result)
    }

    @Test
    fun `formatLiteRtDisplayLabel avoids duplicating tier when displayName contains English tier prefix`() {
        val pack = InstalledLocalAiPackInfo(
            packId = "qwen2.5-1.5b-litert-v1",
            displayName = "Balanced (Qwen 2.5 1.5B)",
            tier = ModelPackTier.BALANCED
        )

        val englishResult = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = "Balanced"
        )
        val spanishResult = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = "Equilibrado"
        )

        assertEquals("LiteRT-LM · Balanced (Qwen 2.5 1.5B)", englishResult)
        assertEquals("LiteRT-LM · Balanced (Qwen 2.5 1.5B)", spanishResult)
    }

    @Test
    fun `formatLiteRtDisplayLabel includes localized tier when displayName is model name only`() {
        val pack = InstalledLocalAiPackInfo(
            packId = "qwen3-0.6b-litert-v1",
            displayName = "Qwen 3 0.6B",
            tier = ModelPackTier.COMPACT
        )

        val result = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = "Compacto"
        )

        assertEquals("LiteRT-LM · Compacto (Qwen 3 0.6B)", result)
    }

    @Test
    fun `formatLiteRtDisplayLabel falls back to localized tier or packId when displayName is blank`() {
        val pack = InstalledLocalAiPackInfo(
            packId = "qwen3-0.6b-litert-v1",
            displayName = "   ",
            tier = ModelPackTier.COMPACT
        )

        val withTier = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = "Compacto"
        )
        val withoutTier = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = null
        )

        assertEquals("LiteRT-LM · Compacto", withTier)
        assertEquals("LiteRT-LM · qwen3-0.6b-litert-v1", withoutTier)
    }

    @Test
    fun `formatLiteRtDisplayLabel appends displayName when localizedTierLabel is null`() {
        val pack = InstalledLocalAiPackInfo(
            packId = "qwen3-0.6b-litert-v1",
            displayName = "Qwen 3 0.6B",
            tier = null
        )

        val result = formatLiteRtDisplayLabel(
            baseLabel = "LiteRT-LM",
            installedPack = pack,
            localizedTierLabel = null
        )

        assertEquals("LiteRT-LM · Qwen 3 0.6B", result)
    }
}
