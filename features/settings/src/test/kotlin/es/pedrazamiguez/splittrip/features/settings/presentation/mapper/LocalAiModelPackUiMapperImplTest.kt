package es.pedrazamiguez.splittrip.features.settings.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.LocalAiModelPackUiMapperImpl
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
                ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                    4096,
                    2048L,
                    1_073_741_824L,
                    4_000_000_000L
                )
            ),
            showDeleteConfirmation = false
        )
        assertEquals(UiText.StringResource(R.string.local_ai_pack_incompatible_ram, 4096), ramState.statusDescription)
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
                ModelPackStatus.Incompatible(
                    ModelPackIncompatibilityReason.PACK_DISABLED,
                    4096,
                    8192L,
                    1_073_741_824L,
                    4_000_000_000L
                )
            ),
            showDeleteConfirmation = false
        )
        assertEquals(
            UiText.StringResource(R.string.local_ai_pack_incompatible_disabled),
            disabledState.statusDescription
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
}
