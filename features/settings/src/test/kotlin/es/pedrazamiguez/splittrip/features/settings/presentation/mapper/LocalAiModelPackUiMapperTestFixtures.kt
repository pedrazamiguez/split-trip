package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import java.util.Locale

internal val usTestLocaleProvider: LocaleProvider = object : LocaleProvider {
    override fun getCurrentLocale(): Locale = Locale.US
}

internal val esTestLocaleProvider: LocaleProvider = object : LocaleProvider {
    override fun getCurrentLocale(): Locale = Locale.forLanguageTag("es-ES")
}

internal val testCompatibleResult = DeviceCompatibilityResult(
    isCompatible = true,
    reason = null,
    availableRamMb = 8192L,
    requiredRamMb = 4096,
    availableStorageBytes = 4_000_000_000L,
    requiredStorageBytes = 1_073_741_824L,
    supportedAbis = listOf("arm64-v8a")
)

internal fun testStateWith(
    status: ModelPackStatus,
    isEntitled: Boolean = true,
    compatibility: DeviceCompatibilityResult = testCompatibleResult,
    activeEngine: AiEngineType = AiEngineType.AI_CORE_GEMMA_4,
    isAiCoreSupported: Boolean = true
): LocalAiModelPackState = LocalAiModelPackState(
    status = status,
    compatibility = compatibility,
    isEntitled = isEntitled,
    isWifiOnly = true,
    isAiCoreSupported = isAiCoreSupported,
    activeEngine = activeEngine
)

internal fun testTierConfig(
    packId: String,
    displayName: String,
    tier: ModelPackTier,
    priority: Int,
    minRamMb: Int
): ModelPackConfig = ModelPackConfig(
    packId = packId,
    displayName = displayName,
    tier = tier,
    priority = priority,
    version = "1.0.0",
    downloadUrl = "https://models.splittrip.eu/$packId.litertlm",
    sha256Checksum = "a".repeat(64),
    sizeBytes = 343_490_560L,
    minRamMb = minRamMb,
    minFreeStorageBytes = 1_073_741_824L,
    isEnabled = true
)

internal fun testCatalogEntry(
    config: ModelPackConfig,
    status: ModelPackStatus,
    isRecommended: Boolean = false,
    isInstalled: Boolean = false,
    isActiveDownload: Boolean = false,
    incompatibleReason: ModelPackIncompatibilityReason? = null
): ModelPackCatalogEntry = ModelPackCatalogEntry(
    config = config,
    compatibility = testCompatibleResult.copy(
        isCompatible = incompatibleReason == null,
        reason = incompatibleReason,
        requiredRamMb = config.minRamMb,
        requiredStorageBytes = config.minFreeStorageBytes
    ),
    status = status,
    isRecommended = isRecommended,
    isInstalled = isInstalled,
    isActiveDownload = isActiveDownload
)
