package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.expense.R
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LocalAiPackBannerUiMapperTest {

    private val usLocaleProvider = object : LocaleProvider {
        override fun getCurrentLocale(): Locale = Locale.US
    }

    private val compatibleResult = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 4096,
        availableStorageBytes = 4_000_000_000L,
        requiredStorageBytes = 1_073_741_824L,
        supportedAbis = listOf("arm64-v8a")
    )

    private val mapper = LocalAiPackBannerUiMapper(usLocaleProvider)

    @Test
    fun `returns null when isOptInBannerEligible is false`() {
        val freeState = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 524_288_000L),
            compatibility = compatibleResult,
            isEntitled = false,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )
        assertNull(mapper.map(freeState))

        val installedState = freeState.copy(
            isEntitled = true,
            status = ModelPackStatus.Installed("1.0.0", 524_288_000L)
        )
        assertNull(mapper.map(installedState))
    }

    @Test
    fun `returns enable-offline-AI copy when AICore is unavailable`() {
        val state = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 524_288_000L),
            compatibility = compatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )

        val banner = mapper.map(state)

        assertNotNull(banner)
        assertEquals(UiText.StringResource(R.string.expense_local_ai_banner_enable_title), banner?.title)
        assertEquals(UiText.StringResource(R.string.expense_local_ai_banner_enable_description), banner?.description)
        assertEquals("500 MB", banner?.sizeBadge)
    }

    @Test
    fun `returns null when AICore is supported`() {
        val state = LocalAiModelPackState(
            status = ModelPackStatus.NotInstalled("1.0.0", 1_610_612_736L),
            compatibility = compatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = true,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4
        )

        val banner = mapper.map(state)

        assertNull(banner)
    }

    @Test
    fun `returns banner with recommended compatible pack size when larger catalog tiers are incompatible`() {
        val compactEntry = ModelPackCatalogEntry(
            config = ModelPackConfig(
                packId = "qwen3-0.6b-litert-v1",
                displayName = "Compact (Qwen 3 0.6B)",
                tier = ModelPackTier.COMPACT,
                priority = 10,
                version = "1.0.0",
                downloadUrl = "https://models.splittrip.eu/compact.litertlm",
                sha256Checksum = "a".repeat(64),
                sizeBytes = 343_490_560L,
                minRamMb = 2048,
                minFreeStorageBytes = 1_073_741_824L,
                isEnabled = true
            ),
            compatibility = compatibleResult,
            status = ModelPackStatus.NotInstalled("1.0.0", 343_490_560L, "qwen3-0.6b-litert-v1"),
            isRecommended = true,
            isInstalled = false,
            isActiveDownload = false
        )
        val highEntry = compactEntry.copy(
            config = compactEntry.config.copy(
                packId = "gemma4-e2b-litert-v1",
                tier = ModelPackTier.HIGH_QUALITY,
                priority = 30,
                sizeBytes = 2_588_147_712L
            ),
            compatibility = compatibleResult.copy(
                isCompatible = false,
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
            ),
            status = ModelPackStatus.Incompatible(
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                requiredRamMb = 6144,
                availableRamMb = 2048L,
                requiredStorageBytes = 5_368_709_120L,
                availableStorageBytes = 8_000_000_000L,
                packId = "gemma4-e2b-litert-v1"
            ),
            isRecommended = false
        )
        val state = LocalAiModelPackState(
            status = compactEntry.status,
            compatibility = compatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4,
            catalogEntries = listOf(compactEntry, highEntry),
            recommendedPackId = compactEntry.config.packId
        )

        val banner = mapper.map(state)

        assertNotNull(banner)
        assertEquals("327.6 MB", banner?.sizeBadge)
    }

    @Test
    fun `returns null when every enabled catalog pack is incompatible`() {
        val incompatibleResult = compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
        )
        val incompatibleEntry = ModelPackCatalogEntry(
            config = ModelPackConfig(
                packId = "qwen3-0.6b-litert-v1",
                tier = ModelPackTier.COMPACT,
                priority = 10,
                version = "1.0.0",
                downloadUrl = "https://models.splittrip.eu/compact.litertlm",
                sha256Checksum = "a".repeat(64),
                sizeBytes = 343_490_560L,
                minRamMb = 2048,
                minFreeStorageBytes = 1_073_741_824L,
                isEnabled = true
            ),
            compatibility = incompatibleResult,
            status = ModelPackStatus.Incompatible(
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                requiredRamMb = 2048,
                availableRamMb = 1024L,
                requiredStorageBytes = 1_073_741_824L,
                availableStorageBytes = 4_000_000_000L,
                packId = "qwen3-0.6b-litert-v1"
            ),
            isRecommended = false,
            isInstalled = false,
            isActiveDownload = false
        )
        val state = LocalAiModelPackState(
            status = incompatibleEntry.status,
            compatibility = incompatibleResult,
            isEntitled = true,
            isWifiOnly = true,
            isAiCoreSupported = false,
            activeEngine = AiEngineType.AI_CORE_GEMMA_4,
            catalogEntries = listOf(incompatibleEntry)
        )

        assertNull(mapper.map(state))
    }
}
