package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

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
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.LocalAiModelPackUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat

class LocalAiModelPackUiMapperImpl(
    private val localeProvider: LocaleProvider
) : LocalAiModelPackUiMapper {

    @Suppress("LongMethod")
    override fun map(
        state: LocalAiModelPackState,
        showDeleteConfirmation: Boolean
    ): LocalAiModelPackUiState {
        val compatibilityLabel = mapCompatibilityLabel(state.compatibility)
        val compatibilityStyle = if (state.compatibility.isCompatible) {
            PassportTagStyle.SUCCESS
        } else {
            PassportTagStyle.WARNING
        }
        val activeEngineLabel = mapActiveEngineLabel(state.activeEngine, state.isAiCoreSupported)

        val baseState = LocalAiModelPackUiState(
            isLoading = false,
            isCompatible = state.compatibility.isCompatible,
            compatibilityLabel = compatibilityLabel,
            compatibilityTagStyle = compatibilityStyle,
            activeEngineLabel = activeEngineLabel,
            isEntitled = state.isEntitled,
            isWifiOnly = state.isWifiOnly,
            showDeleteConfirmation = showDeleteConfirmation
        )

        return when (val status = state.status) {
            is ModelPackStatus.NotInstalled -> mapNotInstalled(baseState, status, state.isEntitled)
            is ModelPackStatus.Downloading -> mapDownloading(baseState, status)
            is ModelPackStatus.Verifying -> mapVerifying(baseState, status)
            is ModelPackStatus.Installed -> mapInstalled(baseState, status, state.isEntitled)
            is ModelPackStatus.Incompatible -> mapIncompatible(baseState, status)
            is ModelPackStatus.Error -> mapError(baseState, status, state.isEntitled)
        }
    }

    private fun mapNotInstalled(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.NotInstalled,
        isEntitled: Boolean
    ): LocalAiModelPackUiState {
        val sizeLabel = status.downloadSizeBytes.takeIf { it > 0L }?.let(::formatByteSize)
        val versionLabel = status.availableVersion.takeIf { it.isNotBlank() }?.let {
            UiText.StringResource(R.string.local_ai_pack_version_format, it)
        }

        return if (!isEntitled) {
            baseState.copy(
                statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_pro_feature),
                statusTagStyle = PassportTagStyle.WARNING,
                statusTitle = UiText.StringResource(R.string.local_ai_pack_status_pro_required_title),
                statusDescription = UiText.StringResource(R.string.local_ai_pack_status_pro_required_desc),
                packVersionLabel = versionLabel,
                packSizeLabel = sizeLabel,
                showUpgradeButton = true
            )
        } else {
            baseState.copy(
                statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_not_installed),
                statusTagStyle = PassportTagStyle.NEUTRAL,
                statusTitle = UiText.StringResource(R.string.local_ai_pack_status_not_installed_title),
                statusDescription = UiText.StringResource(R.string.local_ai_pack_status_not_installed_desc),
                packVersionLabel = versionLabel,
                packSizeLabel = sizeLabel,
                showDownloadButton = true
            )
        }
    }

    private fun mapDownloading(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Downloading
    ): LocalAiModelPackUiState {
        val fraction = BigDecimal(status.progressPercent.coerceIn(0, MAX_PERCENT))
            .divide(BigDecimal(MAX_PERCENT), FRACTION_SCALE, RoundingMode.HALF_UP)
            .toFloat()
        val progressLabel = UiText.StringResource(
            R.string.local_ai_pack_progress_format,
            status.progressPercent,
            formatByteSize(status.downloadedBytes),
            formatByteSize(status.totalBytes)
        )
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_downloading),
            statusTagStyle = PassportTagStyle.INFO,
            statusTitle = UiText.StringResource(R.string.local_ai_pack_status_downloading_title),
            statusDescription = UiText.StringResource(R.string.local_ai_pack_status_downloading_desc),
            isDownloading = true,
            progressFraction = fraction,
            progressLabel = progressLabel,
            showCancelButton = true
        )
    }

    private fun mapVerifying(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Verifying
    ): LocalAiModelPackUiState = baseState.copy(
        statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_verifying),
        statusTagStyle = PassportTagStyle.INFO,
        statusTitle = UiText.StringResource(R.string.local_ai_pack_status_verifying_title),
        statusDescription = UiText.StringResource(R.string.local_ai_pack_status_verifying_desc),
        packVersionLabel = UiText.StringResource(R.string.local_ai_pack_version_format, status.version),
        isVerifying = true
    )

    private fun mapInstalled(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Installed,
        isEntitled: Boolean
    ): LocalAiModelPackUiState = baseState.copy(
        statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_installed),
        statusTagStyle = PassportTagStyle.SUCCESS,
        statusTitle = UiText.StringResource(R.string.local_ai_pack_status_installed_title),
        statusDescription = if (status.isUpdateAvailable) {
            UiText.StringResource(R.string.local_ai_pack_status_update_available_desc)
        } else {
            UiText.StringResource(R.string.local_ai_pack_status_installed_desc)
        },
        packVersionLabel = UiText.StringResource(R.string.local_ai_pack_version_format, status.version),
        packSizeLabel = formatByteSize(status.sizeBytes),
        isInstalled = true,
        isUpdateAvailable = status.isUpdateAvailable,
        showDownloadButton = status.isUpdateAvailable && isEntitled,
        showDeleteButton = true
    )

    private fun mapIncompatible(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Incompatible
    ): LocalAiModelPackUiState = baseState.copy(
        statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_incompatible),
        statusTagStyle = PassportTagStyle.WARNING,
        statusTitle = UiText.StringResource(R.string.local_ai_pack_status_incompatible_title),
        statusDescription = mapIncompatibilityReason(
            reason = status.reason,
            requiredRamMb = status.requiredRamMb,
            requiredStorageBytes = status.requiredStorageBytes
        )
    )

    private fun mapError(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Error,
        isEntitled: Boolean
    ): LocalAiModelPackUiState {
        val errorText = mapErrorReason(status.reason)
        val requiresUpgrade = !isEntitled || status.reason == ModelPackErrorReason.NOT_ENTITLED
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_error),
            statusTagStyle = PassportTagStyle.ERROR,
            statusTitle = UiText.StringResource(R.string.local_ai_pack_status_error_title),
            statusDescription = errorText,
            errorMessage = errorText,
            showRetryButton = status.canRetry && !requiresUpgrade,
            showUpgradeButton = requiresUpgrade
        )
    }

    private fun mapCompatibilityLabel(compatibility: DeviceCompatibilityResult): UiText =
        if (compatibility.isCompatible) {
            UiText.StringResource(R.string.local_ai_pack_compatibility_supported)
        } else {
            mapIncompatibilityReason(
                reason = compatibility.reason ?: ModelPackIncompatibilityReason.PACK_DISABLED,
                requiredRamMb = compatibility.requiredRamMb,
                requiredStorageBytes = compatibility.requiredStorageBytes
            )
        }

    private fun mapIncompatibilityReason(
        reason: ModelPackIncompatibilityReason,
        requiredRamMb: Int,
        requiredStorageBytes: Long
    ): UiText = when (reason) {
        ModelPackIncompatibilityReason.UNSUPPORTED_ABI ->
            UiText.StringResource(R.string.local_ai_pack_incompatible_abi)
        ModelPackIncompatibilityReason.INSUFFICIENT_RAM ->
            UiText.StringResource(R.string.local_ai_pack_incompatible_ram, requiredRamMb)
        ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE ->
            UiText.StringResource(
                R.string.local_ai_pack_incompatible_storage,
                formatByteSize(requiredStorageBytes)
            )
        ModelPackIncompatibilityReason.PACK_DISABLED ->
            UiText.StringResource(R.string.local_ai_pack_incompatible_disabled)
    }

    private fun mapErrorReason(reason: ModelPackErrorReason): UiText = when (reason) {
        ModelPackErrorReason.NOT_ENTITLED ->
            UiText.StringResource(R.string.local_ai_pack_error_not_entitled)
        ModelPackErrorReason.NETWORK_ERROR ->
            UiText.StringResource(R.string.local_ai_pack_error_network)
        ModelPackErrorReason.WIFI_REQUIRED ->
            UiText.StringResource(R.string.local_ai_pack_error_wifi_required)
        ModelPackErrorReason.INSUFFICIENT_STORAGE ->
            UiText.StringResource(R.string.local_ai_pack_error_storage)
        ModelPackErrorReason.CHECKSUM_MISMATCH ->
            UiText.StringResource(R.string.local_ai_pack_error_checksum)
        ModelPackErrorReason.INVALID_CONFIG ->
            UiText.StringResource(R.string.local_ai_pack_error_invalid_config)
        ModelPackErrorReason.IO_ERROR ->
            UiText.StringResource(R.string.local_ai_pack_error_io)
        ModelPackErrorReason.CANCELLED ->
            UiText.StringResource(R.string.local_ai_pack_error_cancelled)
        ModelPackErrorReason.UNKNOWN ->
            UiText.StringResource(R.string.local_ai_pack_error_unknown)
    }

    private fun mapActiveEngineLabel(engine: AiEngineType, isAiCoreSupported: Boolean): UiText = when (engine) {
        AiEngineType.LITE_RT_LM -> UiText.StringResource(R.string.local_ai_pack_engine_lite_rt)
        AiEngineType.AI_CORE_GEMMA_4 -> if (isAiCoreSupported) {
            UiText.StringResource(R.string.local_ai_pack_engine_ai_core)
        } else {
            UiText.StringResource(R.string.local_ai_pack_engine_unavailable)
        }
    }

    private fun formatByteSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        val numberFormat = NumberFormat.getNumberInstance(localeProvider.getCurrentLocale()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
        }
        return if (safeBytes >= BYTES_PER_GB) {
            val gbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_GB), 1, RoundingMode.HALF_UP)
            "${numberFormat.format(gbValue)} GB"
        } else {
            val mbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_MB), 1, RoundingMode.HALF_UP)
            "${numberFormat.format(mbValue)} MB"
        }
    }

    private companion object {
        private const val MAX_PERCENT = 100
        private const val FRACTION_SCALE = 2
        private const val BYTES_PER_MB = 1_048_576L
        private const val BYTES_PER_GB = 1_073_741_824L
    }
}
