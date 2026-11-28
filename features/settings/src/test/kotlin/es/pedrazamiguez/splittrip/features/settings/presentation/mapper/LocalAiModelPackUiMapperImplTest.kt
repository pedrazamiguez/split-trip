package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.LocalAiModelPackUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalAiModelPackUiMapperImplTest {

    private val usLocaleProvider: LocaleProvider = usTestLocaleProvider
    private val esLocaleProvider: LocaleProvider = esTestLocaleProvider
    private val compatibleResult: DeviceCompatibilityResult = testCompatibleResult

    private fun stateWith(
        status: ModelPackStatus,
        isEntitled: Boolean = true,
        compatibility: DeviceCompatibilityResult = compatibleResult,
        activeEngine: AiEngineType = AiEngineType.AI_CORE_GEMMA_4,
        isAiCoreSupported: Boolean = true
    ) = testStateWith(status, isEntitled, compatibility, activeEngine, isAiCoreSupported)

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
            UiText.StringResource(R.string.local_ai_pack_progress_format, 50, "250.0 MB", "500.0 MB"),
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
        assertEquals(UiText.StringResource(R.string.local_ai_pack_incompatible_ram, "4 GB"), ramState.statusDescription)
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
            UiText.StringResource(R.string.local_ai_pack_requirements_format, "2 GB", "1 GB"),
            compactUi.requirementsLabel
        )
        assertEquals(LocalAiModelPackTierAction.DOWNLOAD, compactUi.action)
        assertTrue(compactUi.isActionEnabled)

        val highUi = uiState.catalogTiers[1]
        assertEquals(PassportTagStyle.WARNING, highUi.statusTagStyle)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_incompatible_ram, "6 GB"),
            highUi.incompatibilityReason
        )
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

    @Test
    fun `maps InstallingRuntime with null progress to indeterminate verifying progress with cancel action`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val status = ModelPackStatus.InstallingRuntime(
            progressPercent = null,
            packId = "qwen3-0.6b-litert-v1"
        )
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            isActiveDownload = true,
            status = status
        )
        val uiState = mapper.map(
            state = stateWith(status = status, isEntitled = true).copy(
                catalogEntries = listOf(compact),
                recommendedPackId = compact.config.packId
            ),
            showDeleteConfirmation = false
        )

        assertEquals(PassportTagStyle.INFO, uiState.statusTagStyle)
        assertEquals(UiText.StringResource(R.string.local_ai_pack_badge_installing_runtime), uiState.statusBadge)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_status_installing_runtime_title),
            uiState.statusTitle
        )
        assertFalse(uiState.isDownloading)
        assertTrue(uiState.isVerifying)
        assertTrue(uiState.showCancelButton)
        assertEquals(LocalAiModelPackTierAction.CANCEL, uiState.catalogTiers[0].action)
        assertTrue(uiState.catalogTiers[0].isVerifying)
    }

    @Test
    fun `maps InstallingRuntime with determinate progress to downloading progress bar and catalog tier state`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val status = ModelPackStatus.InstallingRuntime(
            progressPercent = 65,
            packId = "qwen3-0.6b-litert-v1"
        )
        val compact = catalogEntry(
            config = tierConfig("qwen3-0.6b-litert-v1", "Compact (Qwen 3 0.6B)", ModelPackTier.COMPACT, 10, 2048),
            isActiveDownload = true,
            status = status
        )
        val uiState = mapper.map(
            state = stateWith(status = status, isEntitled = true).copy(
                catalogEntries = listOf(compact),
                recommendedPackId = compact.config.packId
            ),
            showDeleteConfirmation = false
        )

        assertTrue(uiState.isDownloading)
        assertFalse(uiState.isVerifying)
        assertEquals(0.65f, uiState.progressFraction)
        assertTrue(uiState.showCancelButton)
        assertTrue(uiState.catalogTiers[0].isDownloading)
        assertEquals(0.65f, uiState.catalogTiers[0].progressFraction)
        assertEquals(LocalAiModelPackTierAction.CANCEL, uiState.catalogTiers[0].action)
    }

    @Test
    fun `maps Error RUNTIME_UNAVAILABLE to runtime unavailable message with retry button`() {
        val mapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val uiState = mapper.map(
            state = stateWith(
                status = ModelPackStatus.Error(
                    reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                    canRetry = true,
                    packId = "qwen3-0.6b-litert-v1"
                ),
                isEntitled = true
            ),
            showDeleteConfirmation = false
        )

        assertEquals(PassportTagStyle.ERROR, uiState.statusTagStyle)
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_error_runtime_unavailable),
            uiState.errorMessage
        )
        assertTrue(uiState.showRetryButton)
        assertFalse(uiState.showUpgradeButton)
    }

    @Test
    fun `maps Downloading progress with fixed 1-decimal precision across locales for status card and catalog tier`() {
        val esMapper = LocalAiModelPackUiMapperImpl(esLocaleProvider)
        val usMapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        fun assertProgress(
            mapper: LocalAiModelPackUiMapperImpl,
            percent: Int,
            downloaded: Long,
            total: Long,
            expectedLabelPair: Pair<String, String>
        ) {
            val status = ModelPackStatus.Downloading(percent, downloaded, total, "qwen2.5-1.5b-litert-v1")
            val entry = testCatalogEntry(
                testTierConfig("qwen2.5-1.5b-litert-v1", "Balanced", ModelPackTier.BALANCED, 20, 3072),
                status = status,
                isActiveDownload = true
            )
            val ui = mapper.map(
                stateWith(status).copy(catalogEntries = listOf(entry), recommendedPackId = entry.config.packId),
                false
            )
            val expected = UiText.StringResource(
                R.string.local_ai_pack_progress_format,
                percent,
                expectedLabelPair.first,
                expectedLabelPair.second
            )
            assertEquals(expected, ui.progressLabel)
            assertEquals(expected, ui.catalogTiers[0].progressLabel)
        }
        assertProgress(esMapper, 9, 153_092_096L, 1_610_612_736L, "146,0 MB" to "1,5 GB")
        assertProgress(esMapper, 2, 38_168_166L, 1_610_612_736L, "36,4 MB" to "1,5 GB")
        assertProgress(esMapper, 50, 1_073_741_824L, 2_147_483_648L, "1,0 GB" to "2,0 GB")
        assertProgress(usMapper, 14, 153_092_096L, 1_073_741_824L, "146.0 MB" to "1.0 GB")
        assertProgress(usMapper, 0, 0L, 524_288_000L, "0.0 MB" to "500.0 MB")
    }

    @Test
    fun `formats RAM requirements and incompatibility reasons in GB or MB while keeping static sizes compact`() {
        val usMapper = LocalAiModelPackUiMapperImpl(usLocaleProvider)
        val esMapper = LocalAiModelPackUiMapperImpl(esLocaleProvider)
        fun assertRam(
            mapper: LocalAiModelPackUiMapperImpl,
            ramMb: Int,
            storageBytes: Long,
            expectedPair: Pair<String, String>,
            incompatible: Boolean = false
        ) {
            val cfg = testTierConfig("pack-$ramMb", "Pack", ModelPackTier.BALANCED, 20, ramMb)
                .copy(sizeBytes = 1_073_741_824L, minFreeStorageBytes = storageBytes)
            val status = if (incompatible) {
                ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                    ramMb,
                    256L,
                    storageBytes,
                    4_000_000_000L,
                    cfg.packId
                )
            } else {
                ModelPackStatus.NotInstalled("1.0.0", 1_073_741_824L, cfg.packId)
            }
            val reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM.takeIf { incompatible }
            val entry = testCatalogEntry(cfg, status, incompatibleReason = reason)
            val ui = mapper.map(stateWith(status).copy(catalogEntries = listOf(entry)), false)
            assertEquals("1 GB", ui.catalogTiers[0].sizeLabel)
            assertEquals(
                UiText.StringResource(
                    R.string.local_ai_pack_requirements_format,
                    expectedPair.first,
                    expectedPair.second
                ),
                ui.catalogTiers[0].requirementsLabel
            )
            if (incompatible) {
                val msg = UiText.StringResource(R.string.local_ai_pack_incompatible_ram, expectedPair.first)
                assertEquals(msg, ui.statusDescription)
                assertEquals(msg, ui.catalogTiers[0].incompatibilityReason)
            }
        }
        assertRam(usMapper, 3072, 3_221_225_472L, "3 GB" to "3 GB")
        assertRam(usMapper, 512, 524_288_000L, "512 MB" to "500 MB", incompatible = true)
        assertRam(usMapper, 1536, 1_073_741_824L, "1.5 GB" to "1 GB")
        assertRam(esMapper, 1536, 1_610_612_736L, "1,5 GB" to "1,5 GB", incompatible = true)
        listOf(0, -128).forEach { assertRam(esMapper, it, 1_073_741_824L, "0 MB" to "1 GB", incompatible = true) }
    }

    private fun tierConfig(
        packId: String,
        displayName: String,
        tier: ModelPackTier,
        priority: Int,
        minRamMb: Int
    ): ModelPackConfig = testTierConfig(packId, displayName, tier, priority, minRamMb)

    private fun catalogEntry(
        config: ModelPackConfig,
        status: ModelPackStatus,
        isRecommended: Boolean = false,
        isInstalled: Boolean = false,
        isActiveDownload: Boolean = false,
        incompatibleReason: ModelPackIncompatibilityReason? = null
    ): ModelPackCatalogEntry = testCatalogEntry(
        config,
        status,
        isRecommended,
        isInstalled,
        isActiveDownload,
        incompatibleReason
    )
}
