package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.LocalAiModelPackUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierAction
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalAiModelPackUiMapperImplTest {

    private val usLocaleProvider = object : LocaleProvider {
        override fun getCurrentLocale(): Locale = Locale.US
    }

    private val esLocaleProvider = object : LocaleProvider {
        override fun getCurrentLocale(): Locale = Locale("es", "ES")
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

    private fun stateWith(
        status: ModelPackStatus,
        isEntitled: Boolean = true,
        compatibility: DeviceCompatibilityResult = compatibleResult,
        activeEngine: AiEngineType = AiEngineType.AI_CORE_GEMMA_4,
        isAiCoreSupported: Boolean = true
    ) = LocalAiModelPackState(
        status = status,
        compatibility = compatibility,
        isEntitled = isEntitled,
        isWifiOnly = true,
        isAiCoreSupported = isAiCoreSupported,
        activeEngine = activeEngine
    )

    @Test
    fun `maps NotInstalled for Pro user with formatted size and enabled download button`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(ModelPackStatus.NotInstalled("1.0.0", 524_288_000L), isEntitled = true),
            showDeleteConfirmation = false
        )

        assertFalse(uiState.isLoading)
        assertEquals(PassportTagStyle.NEUTRAL, uiState.statusTagStyle)
        assertEquals("500 MB", uiState.packSizeLabel)
        assertTrue(uiState.showDownloadButton)
        assertFalse(uiState.showUpgradeButton)
    }

    @Test
    fun `maps NotInstalled for Free user with Pro badge and upgrade CTA instead of download`() {
        val mapper = LocalAiModelPackUiMapperImpl(esLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(ModelPackStatus.NotInstalled("1.0.0", 1_610_612_736L), isEntitled = false),
            showDeleteConfirmation = false
        )

        assertEquals(PassportTagStyle.WARNING, uiState.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_badge_pro_feature), uiState.statusBadge)
        assertEquals("1,5 GB", uiState.packSizeLabel)
        assertFalse(uiState.showDownloadButton)
        assertTrue(uiState.showUpgradeButton)
    }

    @Test
    fun `maps Downloading with percentage fraction and formatted bytes progress`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(
                ModelPackStatus.Downloading(
                    progressPercent = 50,
                    downloadedBytes = 262_144_000L,
                    totalBytes = 524_288_000L
                )
            ),
            showDeleteConfirmation = false
        )

        assertEquals(PassportTagStyle.INFO, uiState.statusTagStyle)
        assertTrue(uiState.isDownloading)
        assertEquals(0.50f, uiState.progressFraction)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_progress_format, 50, "250 MB", "500 MB"),
            uiState.progressLabel
        )
        assertTrue(uiState.showCancelButton)
    }

    @Test
    fun `maps Verifying with indeterminate verifying state`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(ModelPackStatus.Verifying("1.2.0")),
            showDeleteConfirmation = false
        )

        assertEquals(PassportTagStyle.INFO, uiState.statusTagStyle)
        assertTrue(uiState.isVerifying)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_version_format, "1.2.0"), uiState.packVersionLabel)
    }

    @Test
    fun `maps Installed with SUCCESS tag, delete button, and optional update button`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(
                status = ModelPackStatus.Installed("1.0.0", 524_288_000L, isUpdateAvailable = true),
                activeEngine = AiEngineType.LITE_RT_LM
            ),
            showDeleteConfirmation = true
        )

        assertEquals(PassportTagStyle.SUCCESS, uiState.statusTagStyle)
        assertTrue(uiState.isInstalled)
        assertTrue(uiState.isUpdateAvailable)
        assertTrue(uiState.showDownloadButton)
        assertTrue(uiState.showDeleteButton)
        assertTrue(uiState.showDeleteConfirmation)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_engine_lite_rt), uiState.activeEngineLabel)
    }

    @Test
    fun `maps Incompatible ABI and RAM reasons to clear UiText and WARNING tag`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)

        val abiState = mapper.map(
            state = stateWith(
                ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.UNSUPPORTED_ABI,
                    4096,
                    8192L,
                    1_073_741_824L,
                    4_000_000_000L
                )
            ),
            showDeleteConfirmation = false
        )
        assertEquals(PassportTagStyle.WARNING, abiState.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_incompatible_abi), abiState.statusDescription)

        val ramState = mapper.map(
            state = stateWith(
                status = ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                    4096,
                    2048L,
                    1_073_741_824L,
                    4_000_000_000L
                ),
                compatibility = DeviceCompatibilityResult(
                    isCompatible = false,
                    reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                    availableRamMb = 2048L,
                    requiredRamMb = 4096,
                    availableStorageBytes = 4_000_000_000L,
                    requiredStorageBytes = 1_073_741_824L,
                    supportedAbis = listOf("arm64-v8a")
                )
            ),
            showDeleteConfirmation = false
        )
        assertFalse(ramState.isCompatible)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_status_incompatible_title), ramState.statusTitle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_incompatible_ram, 4096), ramState.statusDescription)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_compatibility_unsupported),
            ramState.compatibilityLabel
        )
    }

    @Test
    fun `maps Incompatible storage and disabled reasons to clear UiText`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)

        val storageState = mapper.map(
            state = stateWith(
                ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE,
                    4096,
                    8192L,
                    1_073_741_824L,
                    100_000L
                )
            ),
            showDeleteConfirmation = false
        )
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_incompatible_storage, "1 GB"),
            storageState.statusDescription
        )

        val disabledState = mapper.map(
            state = stateWith(
                status = ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.PACK_DISABLED,
                    4096,
                    8192L,
                    1_073_741_824L,
                    4_000_000_000L
                ),
                compatibility = DeviceCompatibilityResult(
                    isCompatible = false,
                    reason = ModelPackIncompatibilityReason.PACK_DISABLED,
                    availableRamMb = 8192L,
                    requiredRamMb = 4096,
                    availableStorageBytes = 4_000_000_000L,
                    requiredStorageBytes = 1_073_741_824L,
                    supportedAbis = listOf("arm64-v8a")
                )
            ),
            showDeleteConfirmation = false
        )
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_status_disabled_title),
            disabledState.statusTitle
        )
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_incompatible_disabled),
            disabledState.statusDescription
        )
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_compatibility_disabled),
            disabledState.compatibilityLabel
        )
    }

    @Test
    fun `maps Error reasons (network, checksum mismatch, Wi-Fi required) to clear UiText and retry button`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)

        val networkError = mapper.map(
            state = stateWith(ModelPackStatus.Error(ModelPackErrorReason.NETWORK_ERROR, canRetry = true)),
            showDeleteConfirmation = false
        )
        assertEquals(PassportTagStyle.ERROR, networkError.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_error_network), networkError.errorMessage)
        assertTrue(networkError.showRetryButton)
        assertFalse(networkError.showUpgradeButton)

        val checksumError = mapper.map(
            state = stateWith(ModelPackStatus.Error(ModelPackErrorReason.CHECKSUM_MISMATCH)),
            showDeleteConfirmation = false
        )
        assertEquals(UiText.StringResource(R.string.local_ai_pack_error_checksum), checksumError.errorMessage)

        val wifiError = mapper.map(
            state = stateWith(ModelPackStatus.Error(ModelPackErrorReason.WIFI_REQUIRED)),
            showDeleteConfirmation = false
        )
        assertEquals(UiText.StringResource(R.string.local_ai_pack_error_wifi_required), wifiError.errorMessage)

        val entitledError = mapper.map(
            state = stateWith(ModelPackStatus.Error(ModelPackErrorReason.NOT_ENTITLED), isEntitled = false),
            showDeleteConfirmation = false
        )
        assertTrue(entitledError.showUpgradeButton)
        assertFalse(entitledError.showRetryButton)
        assertNotNull(entitledError.errorMessage)

        val okState = mapper.map(
            state = stateWith(ModelPackStatus.NotInstalled("1.0.0", 10_485_760L)),
            showDeleteConfirmation = false
        )
        assertNull(okState.errorMessage)
    }

    @Test
    fun `maps multi-tier catalog with recommended PRIMARY badge, requirements, and incompatibility reasons`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            isRecommended = true,
            status = ModelPackStatus.NotInstalled("1.0.0", 343_490_560L, "qwen3-0.6b-litert-v1")
        )
        val high = catalogEntry(
            config = tierConfig(
                "gemma4-e2b-litert-v1",
                "High Quality (Gemma 4 E2B)",
                ModelPackTier.HIGH_QUALITY,
                30,
                6144
            ),
            incompatibleReason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            status = ModelPackStatus.Incompatible(
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                requiredRamMb = 6144,
                availableRamMb = 2048L,
                requiredStorageBytes = 5_368_709_120L,
                availableStorageBytes = 8_000_000_000L,
                packId = "gemma4-e2b-litert-v1"
            )
        )
        val state = stateWith(
            status = compact.status,
            isEntitled = true
        ).copy(
            catalogEntries = listOf(compact, high),
            recommendedPackId = compact.config.packId
        )

        val uiState = mapper.map(state, showDeleteConfirmation = false)

        assertEquals(2, uiState.catalogTiers.size)
        val compactUi = uiState.catalogTiers[0]
        assertEquals(PassportTagStyle.PRIMARY, compactUi.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_badge_recommended), compactUi.statusBadge)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_requirements_format, 2048, "1 GB"),
            compactUi.requirementsLabel
        )
        assertEquals(LocalAiModelPackTierAction.DOWNLOAD, compactUi.action)
        assertTrue(compactUi.isActionEnabled)

        val highUi = uiState.catalogTiers[1]
        assertEquals(PassportTagStyle.WARNING, highUi.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_incompatible_ram, 6144), highUi.incompatibilityReason)
        assertEquals(LocalAiModelPackTierAction.NONE, highUi.action)
        assertFalse(highUi.isActionEnabled)
    }

    @Test
    fun `maps installed tier with compatible SWITCH tier and incompatible NONE tier`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            isInstalled = true,
            status = ModelPackStatus.Installed("1.0.0", 343_490_560L, packId = "qwen3-0.6b-litert-v1")
        )
        val balanced = catalogEntry(
            config = tierConfig("qwen2.5-1.5b-litert-v1", "Balanced (Qwen 2.5 1.5B)", ModelPackTier.BALANCED, 20, 3072),
            isRecommended = true,
            status = ModelPackStatus.NotInstalled("1.0.0", 1_597_922_856L, "qwen2.5-1.5b-litert-v1")
        )
        val high = catalogEntry(
            config = tierConfig(
                "gemma4-e2b-litert-v1",
                "High Quality (Gemma 4 E2B)",
                ModelPackTier.HIGH_QUALITY,
                30,
                6144
            ),
            incompatibleReason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            status = ModelPackStatus.Incompatible(
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                requiredRamMb = 6144,
                availableRamMb = 4096L,
                requiredStorageBytes = 5_368_709_120L,
                availableStorageBytes = 8_000_000_000L,
                packId = "gemma4-e2b-litert-v1"
            )
        )
        val state = stateWith(
            status = compact.status,
            isEntitled = true,
            activeEngine = AiEngineType.LITE_RT_LM
        ).copy(
            catalogEntries = listOf(compact, balanced, high),
            installedPackId = compact.config.packId,
            recommendedPackId = balanced.config.packId
        )

        val uiState = mapper.map(state, showDeleteConfirmation = false)

        assertEquals(UiText.DynamicString("Compact (Qwen 3 0.6B)"), uiState.activePackName)
        assertEquals(LocalAiModelPackTierAction.NONE, uiState.catalogTiers[0].action)
        assertEquals(PassportTagStyle.SUCCESS, uiState.catalogTiers[0].statusTagStyle)
        assertEquals(LocalAiModelPackTierAction.SWITCH, uiState.catalogTiers[1].action)
        assertTrue(uiState.catalogTiers[1].isActionEnabled)
        assertEquals(LocalAiModelPackTierAction.NONE, uiState.catalogTiers[2].action)
    }

    @Test
    fun `disables other tier actions while one tier is Downloading and exposes CANCEL on active tier`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            status = ModelPackStatus.NotInstalled("1.0.0", 343_490_560L, "qwen3-0.6b-litert-v1")
        )
        val downloadingStatus = ModelPackStatus.Downloading(
            progressPercent = 40,
            downloadedBytes = 400_000_000L,
            totalBytes = 1_000_000_000L,
            packId = "qwen2.5-1.5b-litert-v1"
        )
        val balanced = catalogEntry(
            config = tierConfig("qwen2.5-1.5b-litert-v1", "Balanced (Qwen 2.5 1.5B)", ModelPackTier.BALANCED, 20, 3072),
            isRecommended = true,
            isActiveDownload = true,
            status = downloadingStatus
        )
        val state = stateWith(status = downloadingStatus, isEntitled = true).copy(
            catalogEntries = listOf(compact, balanced),
            recommendedPackId = balanced.config.packId
        )

        val uiState = mapper.map(state, showDeleteConfirmation = false)

        assertEquals(LocalAiModelPackTierAction.DOWNLOAD, uiState.catalogTiers[0].action)
        assertFalse(uiState.catalogTiers[0].isActionEnabled)
        assertEquals(LocalAiModelPackTierAction.CANCEL, uiState.catalogTiers[1].action)
        assertTrue(uiState.catalogTiers[1].isActionEnabled)
    }

    @Test
    fun `maps Error on targeted pack with RETRY action on that pack`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val errorStatus = ModelPackStatus.Error(
            reason = ModelPackErrorReason.NETWORK_ERROR,
            canRetry = true,
            packId = "qwen3-0.6b-litert-v1"
        )
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            status = errorStatus
        )
        val state = stateWith(status = errorStatus, isEntitled = true).copy(
            catalogEntries = listOf(compact)
        )

        val uiState = mapper.map(state, showDeleteConfirmation = false)

        assertEquals(1, uiState.catalogTiers.size)
        assertEquals(PassportTagStyle.ERROR, uiState.catalogTiers[0].statusTagStyle)
        assertEquals(LocalAiModelPackTierAction.RETRY, uiState.catalogTiers[0].action)
        assertTrue(uiState.catalogTiers[0].isActionEnabled)
        assertEquals("qwen3-0.6b-litert-v1", uiState.activeTargetPackId)
    }

    @Test
    fun `maps non-entitled user catalog tiers as previewable with single upgrade CTA and NONE tier actions`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            isRecommended = true,
            status = ModelPackStatus.NotInstalled("1.0.0", 343_490_560L, "qwen3-0.6b-litert-v1")
        )
        val state = stateWith(
            status = compact.status,
            isEntitled = false
        ).copy(
            catalogEntries = listOf(compact),
            recommendedPackId = compact.config.packId
        )

        val uiState = mapper.map(state, showDeleteConfirmation = false)

        assertTrue(uiState.showUpgradeButton)
        assertEquals(1, uiState.catalogTiers.size)
        assertEquals(LocalAiModelPackTierAction.NONE, uiState.catalogTiers[0].action)
        assertFalse(uiState.catalogTiers[0].isActionEnabled)
    }

    private fun tierConfig(
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

    private fun catalogEntry(
        config: ModelPackConfig,
        status: ModelPackStatus,
        isRecommended: Boolean = false,
        isInstalled: Boolean = false,
        isActiveDownload: Boolean = false,
        incompatibleReason: ModelPackIncompatibilityReason? = null
    ): ModelPackCatalogEntry = ModelPackCatalogEntry(
        config = config,
        compatibility = compatibleResult.copy(
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
}
