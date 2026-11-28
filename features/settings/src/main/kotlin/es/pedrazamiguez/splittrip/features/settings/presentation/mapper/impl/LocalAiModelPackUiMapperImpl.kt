package es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.LocalAiModelPackUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierAction
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("TooManyFunctions")
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
        val activeTargetPackId = resolveActiveTargetPackId(state)
        val activePackDisplayName = resolveActivePackDisplayName(state, activeTargetPackId)
        val catalogTiers = mapCatalogTiers(state)

        val baseState = LocalAiModelPackUiState(
            isLoading = false,
            activePackName = activePackDisplayName?.let { UiText.DynamicString(it) },
            activeTargetPackId = activeTargetPackId,
            isCompatible = state.compatibility.isCompatible,
            compatibilityLabel = compatibilityLabel,
            compatibilityTagStyle = compatibilityStyle,
            activeEngineLabel = activeEngineLabel,
            isEntitled = state.isEntitled,
            isWifiOnly = state.isWifiOnly,
            showDeleteConfirmation = showDeleteConfirmation,
            catalogTiers = catalogTiers
        )

        val mapped = when (val status = state.status) {
            is ModelPackStatus.NotInstalled -> mapNotInstalled(baseState, status, state.isEntitled)
            is ModelPackStatus.Downloading -> mapDownloading(baseState, status, activePackDisplayName)
            is ModelPackStatus.Verifying -> mapVerifying(baseState, status, activePackDisplayName)
            is ModelPackStatus.Installed -> mapInstalled(baseState, status, state.isEntitled, activePackDisplayName)
            is ModelPackStatus.Incompatible -> mapIncompatible(baseState, status)
            is ModelPackStatus.Error -> mapError(baseState, status, state.isEntitled)
        }

        val hasInstalledCatalogEntry = state.catalogEntries.any { it.isInstalled }
        return if (hasInstalledCatalogEntry && !mapped.showDeleteButton) {
            mapped.copy(showDeleteButton = true)
        } else {
            mapped
        }
    }

    private fun resolveActiveTargetPackId(state: LocalAiModelPackState): String? = when (val status = state.status) {
        is ModelPackStatus.Downloading -> status.packId.takeIf { it.isNotBlank() } ?: state.recommendedPackId
        is ModelPackStatus.Verifying -> status.packId.takeIf { it.isNotBlank() } ?: state.recommendedPackId
        is ModelPackStatus.Error -> status.packId.takeIf { it.isNotBlank() } ?: state.recommendedPackId
        is ModelPackStatus.Installed -> status.packId.takeIf { it.isNotBlank() } ?: state.installedPackId
        is ModelPackStatus.NotInstalled -> status.packId.takeIf { it.isNotBlank() } ?: state.recommendedPackId
        is ModelPackStatus.Incompatible -> null
    }

    private fun resolveActivePackDisplayName(
        state: LocalAiModelPackState,
        activeTargetPackId: String?
    ): String? {
        if (activeTargetPackId.isNullOrBlank()) return null
        val matchingEntry = state.catalogEntries.firstOrNull { it.config.packId == activeTargetPackId }
            ?: return null
        return matchingEntry.config.displayName.takeIf { it.isNotBlank() } ?: matchingEntry.config.packId
    }

    private fun mapCatalogTiers(state: LocalAiModelPackState): ImmutableList<LocalAiModelPackTierUiModel> {
        if (state.catalogEntries.isEmpty()) return persistentListOf()
        val hasAnyActiveDownload = state.catalogEntries.any { it.isActiveDownload } ||
            state.status is ModelPackStatus.Downloading ||
            state.status is ModelPackStatus.Verifying
        val hasAnyInstalled = state.catalogEntries.any { it.isInstalled } ||
            state.installedPackId != null ||
            state.status is ModelPackStatus.Installed

        return state.catalogEntries.map { entry ->
            mapCatalogEntry(
                entry = entry,
                isEntitled = state.isEntitled,
                hasAnyActiveDownload = hasAnyActiveDownload,
                hasAnyInstalled = hasAnyInstalled
            )
        }.toImmutableList()
    }

    private fun mapCatalogEntry(
        entry: ModelPackCatalogEntry,
        isEntitled: Boolean,
        hasAnyActiveDownload: Boolean,
        hasAnyInstalled: Boolean
    ): LocalAiModelPackTierUiModel {
        val config = entry.config
        val tierLabel = mapTierLabel(config.tier)
        val displayName = if (config.displayName.isNotBlank()) {
            UiText.DynamicString(config.displayName)
        } else {
            tierLabel
        }
        val versionLabel = UiText.StringResource(R.string.local_ai_pack_version_format, config.version)
        val sizeLabel = formatByteSize(config.sizeBytes)
        val requirementsLabel = UiText.StringResource(
            R.string.local_ai_pack_requirements_format,
            formatRamSize(config.minRamMb),
            formatByteSize(config.minFreeStorageBytes)
        )

        val baseTier = LocalAiModelPackTierUiModel(
            packId = config.packId,
            displayName = displayName,
            tierLabel = tierLabel,
            versionLabel = versionLabel,
            sizeLabel = sizeLabel,
            requirementsLabel = requirementsLabel,
            statusBadge = tierLabel,
            statusTagStyle = PassportTagStyle.NEUTRAL,
            isRecommended = entry.isRecommended,
            isInstalled = entry.isInstalled,
            isCompatible = entry.compatibility.isCompatible,
            isDownloading = false,
            isVerifying = false,
            progressFraction = 0f,
            progressLabel = null,
            incompatibilityReason = null,
            action = LocalAiModelPackTierAction.NONE,
            actionLabel = null,
            isActionEnabled = false
        )

        return when (val status = entry.status) {
            is ModelPackStatus.Downloading -> mapDownloadingTier(baseTier, status)
            is ModelPackStatus.Verifying -> baseTier.copy(
                statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_verifying),
                statusTagStyle = PassportTagStyle.INFO,
                isVerifying = true
            )
            is ModelPackStatus.Installed -> mapInstalledTier(baseTier, status, isEntitled, hasAnyActiveDownload)
            is ModelPackStatus.Error -> mapErrorTier(baseTier, status, entry, isEntitled, hasAnyActiveDownload)
            is ModelPackStatus.Incompatible -> mapIncompatibleTier(baseTier, status)
            is ModelPackStatus.NotInstalled -> mapNotInstalledTier(
                baseTier = baseTier,
                entry = entry,
                isEntitled = isEntitled,
                hasAnyActiveDownload = hasAnyActiveDownload,
                hasAnyInstalled = hasAnyInstalled
            )
        }
    }

    private fun mapDownloadingTier(
        baseTier: LocalAiModelPackTierUiModel,
        status: ModelPackStatus.Downloading
    ): LocalAiModelPackTierUiModel = baseTier.copy(
        statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_downloading),
        statusTagStyle = PassportTagStyle.INFO,
        isDownloading = true,
        progressFraction = computeProgressFraction(status.progressPercent),
        progressLabel = UiText.StringResource(
            R.string.local_ai_pack_progress_format,
            status.progressPercent,
            formatByteSize(status.downloadedBytes, minFractionDigits = PROGRESS_MIN_FRACTION_DIGITS),
            formatByteSize(status.totalBytes, minFractionDigits = PROGRESS_MIN_FRACTION_DIGITS)
        ),
        action = LocalAiModelPackTierAction.CANCEL,
        actionLabel = UiText.StringResource(R.string.local_ai_pack_action_cancel),
        isActionEnabled = true
    )

    private fun mapInstalledTier(
        baseTier: LocalAiModelPackTierUiModel,
        status: ModelPackStatus.Installed,
        isEntitled: Boolean,
        hasAnyActiveDownload: Boolean
    ): LocalAiModelPackTierUiModel {
        val canUpdate = status.isUpdateAvailable && isEntitled
        return baseTier.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_installed),
            statusTagStyle = PassportTagStyle.SUCCESS,
            action = if (canUpdate) LocalAiModelPackTierAction.UPDATE else LocalAiModelPackTierAction.NONE,
            actionLabel = if (canUpdate) UiText.StringResource(R.string.local_ai_pack_action_update) else null,
            isActionEnabled = canUpdate && !hasAnyActiveDownload
        )
    }

    private fun mapErrorTier(
        baseTier: LocalAiModelPackTierUiModel,
        status: ModelPackStatus.Error,
        entry: ModelPackCatalogEntry,
        isEntitled: Boolean,
        hasAnyActiveDownload: Boolean
    ): LocalAiModelPackTierUiModel {
        val canRetry = isEntitled && status.canRetry && entry.compatibility.isCompatible
        return baseTier.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_error),
            statusTagStyle = PassportTagStyle.ERROR,
            action = if (canRetry) LocalAiModelPackTierAction.RETRY else LocalAiModelPackTierAction.NONE,
            actionLabel = if (canRetry) UiText.StringResource(R.string.local_ai_pack_action_retry) else null,
            isActionEnabled = canRetry && !hasAnyActiveDownload
        )
    }

    private fun mapIncompatibleTier(
        baseTier: LocalAiModelPackTierUiModel,
        status: ModelPackStatus.Incompatible
    ): LocalAiModelPackTierUiModel = baseTier.copy(
        statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_incompatible),
        statusTagStyle = PassportTagStyle.WARNING,
        incompatibilityReason = mapIncompatibilityReason(
            reason = status.reason,
            requiredRamMb = status.requiredRamMb,
            requiredStorageBytes = status.requiredStorageBytes
        )
    )

    private fun mapNotInstalledTier(
        baseTier: LocalAiModelPackTierUiModel,
        entry: ModelPackCatalogEntry,
        isEntitled: Boolean,
        hasAnyActiveDownload: Boolean,
        hasAnyInstalled: Boolean
    ): LocalAiModelPackTierUiModel {
        val badge = if (entry.isRecommended) {
            UiText.StringResource(R.string.local_ai_pack_badge_recommended)
        } else {
            UiText.StringResource(R.string.local_ai_pack_badge_compatible)
        }
        val style = if (entry.isRecommended) PassportTagStyle.PRIMARY else PassportTagStyle.SUCCESS
        val action = when {
            !isEntitled -> LocalAiModelPackTierAction.NONE
            hasAnyInstalled -> LocalAiModelPackTierAction.SWITCH
            else -> LocalAiModelPackTierAction.DOWNLOAD
        }
        val actionLabel = when (action) {
            LocalAiModelPackTierAction.SWITCH -> UiText.StringResource(R.string.local_ai_pack_action_switch)
            LocalAiModelPackTierAction.DOWNLOAD -> UiText.StringResource(R.string.local_ai_pack_action_download)
            else -> null
        }
        return baseTier.copy(
            statusBadge = badge,
            statusTagStyle = style,
            action = action,
            actionLabel = actionLabel,
            isActionEnabled = isEntitled && !hasAnyActiveDownload
        )
    }

    private fun mapNotInstalled(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.NotInstalled,
        isEntitled: Boolean
    ): LocalAiModelPackUiState {
        val sizeLabel = status.downloadSizeBytes.takeIf { it > 0L }?.let { formatByteSize(it) }
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
        status: ModelPackStatus.Downloading,
        activePackDisplayName: String?
    ): LocalAiModelPackUiState {
        val fraction = computeProgressFraction(status.progressPercent)
        val progressLabel = UiText.StringResource(
            R.string.local_ai_pack_progress_format,
            status.progressPercent,
            formatByteSize(status.downloadedBytes, minFractionDigits = PROGRESS_MIN_FRACTION_DIGITS),
            formatByteSize(status.totalBytes, minFractionDigits = PROGRESS_MIN_FRACTION_DIGITS)
        )
        val title = if (activePackDisplayName != null) {
            UiText.StringResource(R.string.local_ai_pack_status_downloading_named_title, activePackDisplayName)
        } else {
            UiText.StringResource(R.string.local_ai_pack_status_downloading_title)
        }
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_downloading),
            statusTagStyle = PassportTagStyle.INFO,
            statusTitle = title,
            statusDescription = UiText.StringResource(R.string.local_ai_pack_status_downloading_desc),
            isDownloading = true,
            progressFraction = fraction,
            progressLabel = progressLabel,
            showCancelButton = true
        )
    }

    private fun mapVerifying(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Verifying,
        activePackDisplayName: String?
    ): LocalAiModelPackUiState {
        val title = if (activePackDisplayName != null) {
            UiText.StringResource(R.string.local_ai_pack_status_verifying_named_title, activePackDisplayName)
        } else {
            UiText.StringResource(R.string.local_ai_pack_status_verifying_title)
        }
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_verifying),
            statusTagStyle = PassportTagStyle.INFO,
            statusTitle = title,
            statusDescription = UiText.StringResource(R.string.local_ai_pack_status_verifying_desc),
            packVersionLabel = UiText.StringResource(R.string.local_ai_pack_version_format, status.version),
            isVerifying = true
        )
    }

    private fun mapInstalled(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Installed,
        isEntitled: Boolean,
        activePackDisplayName: String?
    ): LocalAiModelPackUiState {
        val description = when {
            status.isUpdateAvailable -> UiText.StringResource(R.string.local_ai_pack_status_update_available_desc)
            activePackDisplayName != null ->
                UiText.StringResource(R.string.local_ai_pack_status_installed_named_desc, activePackDisplayName)
            else -> UiText.StringResource(R.string.local_ai_pack_status_installed_desc)
        }
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_installed),
            statusTagStyle = PassportTagStyle.SUCCESS,
            statusTitle = UiText.StringResource(R.string.local_ai_pack_status_installed_title),
            statusDescription = description,
            packVersionLabel = UiText.StringResource(R.string.local_ai_pack_version_format, status.version),
            packSizeLabel = formatByteSize(status.sizeBytes),
            isInstalled = true,
            isUpdateAvailable = status.isUpdateAvailable,
            showDownloadButton = status.isUpdateAvailable && isEntitled,
            showDeleteButton = true
        )
    }

    private fun mapIncompatible(
        baseState: LocalAiModelPackUiState,
        status: ModelPackStatus.Incompatible
    ): LocalAiModelPackUiState {
        val titleRes = if (status.reason == ModelPackIncompatibilityReason.PACK_DISABLED) {
            R.string.local_ai_pack_status_disabled_title
        } else {
            R.string.local_ai_pack_status_incompatible_title
        }
        return baseState.copy(
            statusBadge = UiText.StringResource(R.string.local_ai_pack_badge_incompatible),
            statusTagStyle = PassportTagStyle.WARNING,
            statusTitle = UiText.StringResource(titleRes),
            statusDescription = mapIncompatibilityReason(
                reason = status.reason,
                requiredRamMb = status.requiredRamMb,
                requiredStorageBytes = status.requiredStorageBytes
            ),
            showUpgradeButton = !baseState.isEntitled
        )
    }

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

    private fun mapTierLabel(tier: ModelPackTier): UiText = when (tier) {
        ModelPackTier.COMPACT -> UiText.StringResource(R.string.local_ai_pack_tier_compact)
        ModelPackTier.BALANCED -> UiText.StringResource(R.string.local_ai_pack_tier_balanced)
        ModelPackTier.HIGH_QUALITY -> UiText.StringResource(R.string.local_ai_pack_tier_high_quality)
    }

    private fun mapCompatibilityLabel(compatibility: DeviceCompatibilityResult): UiText =
        when {
            compatibility.isCompatible ->
                UiText.StringResource(R.string.local_ai_pack_compatibility_supported)
            compatibility.reason == ModelPackIncompatibilityReason.PACK_DISABLED ->
                UiText.StringResource(R.string.local_ai_pack_compatibility_disabled)
            else ->
                UiText.StringResource(R.string.local_ai_pack_compatibility_unsupported)
        }

    private fun mapIncompatibilityReason(
        reason: ModelPackIncompatibilityReason,
        requiredRamMb: Int,
        requiredStorageBytes: Long
    ): UiText = when (reason) {
        ModelPackIncompatibilityReason.UNSUPPORTED_ABI ->
            UiText.StringResource(R.string.local_ai_pack_incompatible_abi)
        ModelPackIncompatibilityReason.INSUFFICIENT_RAM ->
            UiText.StringResource(
                R.string.local_ai_pack_incompatible_ram,
                formatRamSize(requiredRamMb)
            )
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

    private fun computeProgressFraction(progressPercent: Int): Float =
        BigDecimal(progressPercent.coerceIn(0, MAX_PERCENT))
            .divide(BigDecimal(MAX_PERCENT), FRACTION_SCALE, RoundingMode.HALF_UP)
            .toFloat()

    private fun formatRamSize(ramMb: Int): String =
        formatByteSize(ramMb.coerceAtLeast(0).toLong() * BYTES_PER_MB)

    private fun formatByteSize(
        bytes: Long,
        minFractionDigits: Int = DEFAULT_MIN_FRACTION_DIGITS
    ): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        val numberFormat = NumberFormat.getNumberInstance(localeProvider.getCurrentLocale()).apply {
            minimumFractionDigits = minFractionDigits
            maximumFractionDigits = BYTE_SIZE_FRACTION_SCALE
        }
        return if (safeBytes >= BYTES_PER_GB) {
            val gbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_GB), BYTE_SIZE_FRACTION_SCALE, RoundingMode.HALF_UP)
            "${numberFormat.format(gbValue)} GB"
        } else {
            val mbValue = BigDecimal(safeBytes)
                .divide(BigDecimal(BYTES_PER_MB), BYTE_SIZE_FRACTION_SCALE, RoundingMode.HALF_UP)
            "${numberFormat.format(mbValue)} MB"
        }
    }

    private companion object {
        private const val MAX_PERCENT = 100
        private const val FRACTION_SCALE = 2
        private const val BYTE_SIZE_FRACTION_SCALE = 1
        private const val DEFAULT_MIN_FRACTION_DIGITS = 0
        private const val PROGRESS_MIN_FRACTION_DIGITS = 1
        private const val BYTES_PER_MB = 1_048_576L
        private const val BYTES_PER_GB = 1_073_741_824L
    }
}
