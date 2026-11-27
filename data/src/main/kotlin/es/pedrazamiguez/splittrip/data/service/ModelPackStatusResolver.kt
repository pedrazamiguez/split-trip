package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.litert.LiteRtDynamicRuntimeState
import es.pedrazamiguez.splittrip.data.litert.isActiveInstall
import es.pedrazamiguez.splittrip.data.litert.toProgressPercent
import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.worker.ModelPackWorkState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus

internal class ModelPackStatusResolver(
    private val compatibilityProvider: LocalAiDeviceCompatibilityProvider
) {

    @Suppress("LongParameterList")
    fun resolveAggregateStatus(
        catalog: ModelPackCatalog,
        evaluated: List<EvaluatedCatalogPack>,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        workState: ModelPackWorkState,
        runtimeState: LiteRtDynamicRuntimeState,
        activeRuntimePackId: String?
    ): ModelPackStatus = when (workState) {
        is ModelPackWorkState.Queued -> {
            val targetConfig = resolveWorkTargetConfig(catalog, workState.packId)
            ModelPackStatus.Downloading(
                progressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = targetConfig?.sizeBytes ?: 0L,
                packId = workState.packId
            )
        }

        is ModelPackWorkState.Downloading -> {
            val targetConfig = resolveWorkTargetConfig(catalog, workState.packId)
            ModelPackStatus.Downloading(
                progressPercent = workState.progressPercent,
                downloadedBytes = workState.downloadedBytes,
                totalBytes = workState.totalBytes.takeIf { it > 0L } ?: targetConfig?.sizeBytes ?: 0L,
                packId = workState.packId
            )
        }

        is ModelPackWorkState.Verifying -> {
            val targetConfig = resolveWorkTargetConfig(catalog, workState.packId)
            ModelPackStatus.Verifying(
                version = workState.version.ifBlank { targetConfig?.version.orEmpty() },
                packId = workState.packId
            )
        }

        is ModelPackWorkState.Failed -> ModelPackStatus.Error(
            reason = workState.reason,
            canRetry = true,
            packId = workState.packId
        )

        ModelPackWorkState.Idle,
        is ModelPackWorkState.Succeeded,
        ModelPackWorkState.Cancelled -> resolveIdleOrRuntimeStatus(
            catalog = catalog,
            evaluated = evaluated,
            installed = installed,
            isInstalledValid = isInstalledValid,
            runtimeState = runtimeState,
            activeRuntimePackId = activeRuntimePackId
        )
    }

    @Suppress("LongParameterList")
    fun mapCatalogEntry(
        entry: EvaluatedCatalogPack,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        workState: ModelPackWorkState,
        runtimeState: LiteRtDynamicRuntimeState,
        activeRuntimePackId: String?
    ): ModelPackCatalogEntry {
        val packId = entry.config.packId
        val isRuntimeInstalled = runtimeState is LiteRtDynamicRuntimeState.Installed
        val isThisInstalled = isInstalledValid && installed?.packId == packId && isRuntimeInstalled
        val isThisActiveWork = workState.targetPackId() == packId
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val targetRuntimePackId = activeRuntimePackId ?: validInstalledPackId
        val isThisActiveRuntime = runtimeState.isActiveInstall() && targetRuntimePackId == packId
        val isThisFailedRuntime = isFailedRuntimeForPack(
            packId = packId,
            activeRuntimePackId = activeRuntimePackId,
            validInstalledPackId = validInstalledPackId,
            runtimeState = runtimeState
        )
        val status = resolveCatalogEntryStatus(
            entry = entry,
            installed = installed,
            isInstalledValid = isInstalledValid,
            isThisInstalled = isThisInstalled,
            isThisActiveWork = isThisActiveWork,
            isThisActiveRuntime = isThisActiveRuntime,
            isThisFailedRuntime = isThisFailedRuntime,
            workState = workState,
            runtimeState = runtimeState
        )
        val isActiveDownload = (isThisActiveWork && workState.isActiveDownload()) || isThisActiveRuntime

        return ModelPackCatalogEntry(
            config = entry.config,
            compatibility = entry.compatibility,
            status = status,
            isRecommended = entry.isRecommended,
            isInstalled = isThisInstalled,
            isActiveDownload = isActiveDownload
        )
    }

    @Suppress("LongParameterList")
    private fun resolveIdleOrRuntimeStatus(
        catalog: ModelPackCatalog,
        evaluated: List<EvaluatedCatalogPack>,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        runtimeState: LiteRtDynamicRuntimeState,
        activeRuntimePackId: String?
    ): ModelPackStatus {
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val activePackId = activeRuntimePackId ?: validInstalledPackId.orEmpty()
        if (runtimeState.isActiveInstall()) {
            return ModelPackStatus.InstallingRuntime(
                progressPercent = runtimeState.toProgressPercent(),
                packId = activePackId
            )
        }
        val hasRuntimeFailureContext = activeRuntimePackId != null || isInstalledValid
        if (runtimeState is LiteRtDynamicRuntimeState.Failed && hasRuntimeFailureContext) {
            return ModelPackStatus.Error(
                reason = runtimeState.reason,
                canRetry = true,
                packId = activePackId
            )
        }
        return resolveSettledStatus(
            catalog = catalog,
            evaluated = evaluated,
            installed = installed,
            isInstalledValid = isInstalledValid,
            runtimeState = runtimeState
        )
    }

    private fun isFailedRuntimeForPack(
        packId: String,
        activeRuntimePackId: String?,
        validInstalledPackId: String?,
        runtimeState: LiteRtDynamicRuntimeState
    ): Boolean {
        if (runtimeState !is LiteRtDynamicRuntimeState.Failed) return false
        return activeRuntimePackId == packId || (activeRuntimePackId == null && validInstalledPackId == packId)
    }

    @Suppress("LongParameterList", "ReturnCount")
    private fun resolveCatalogEntryStatus(
        entry: EvaluatedCatalogPack,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        isThisInstalled: Boolean,
        isThisActiveWork: Boolean,
        isThisActiveRuntime: Boolean,
        isThisFailedRuntime: Boolean,
        workState: ModelPackWorkState,
        runtimeState: LiteRtDynamicRuntimeState
    ): ModelPackStatus {
        val packId = entry.config.packId
        if (isThisActiveWork) {
            resolveActiveWorkStatus(entry.config, workState)?.let { return it }
        }
        if (isThisActiveRuntime) {
            return ModelPackStatus.InstallingRuntime(
                progressPercent = runtimeState.toProgressPercent(),
                packId = packId
            )
        }
        if (isThisFailedRuntime && runtimeState is LiteRtDynamicRuntimeState.Failed) {
            return ModelPackStatus.Error(
                reason = runtimeState.reason,
                canRetry = true,
                packId = packId
            )
        }
        if (isThisInstalled && installed != null) {
            return ModelPackStatus.Installed(
                version = installed.version,
                sizeBytes = installed.sizeBytes,
                isUpdateAvailable = installed.version != entry.config.version,
                packId = packId
            )
        }
        if (isInstalledValid && installed?.packId == packId) {
            return ModelPackStatus.InstallingRuntime(
                progressPercent = null,
                packId = packId
            )
        }
        if (!entry.compatibility.isCompatible) {
            return ModelPackStatus.Incompatible(
                reason = entry.compatibility.reason ?: ModelPackIncompatibilityReason.PACK_DISABLED,
                requiredRamMb = entry.compatibility.requiredRamMb,
                availableRamMb = entry.compatibility.availableRamMb,
                requiredStorageBytes = entry.compatibility.requiredStorageBytes,
                availableStorageBytes = entry.compatibility.availableStorageBytes,
                packId = packId
            )
        }
        return ModelPackStatus.NotInstalled(
            availableVersion = entry.config.version,
            downloadSizeBytes = entry.config.sizeBytes,
            packId = packId
        )
    }

    private fun resolveActiveWorkStatus(
        config: ModelPackConfig,
        workState: ModelPackWorkState
    ): ModelPackStatus? = when (workState) {
        is ModelPackWorkState.Queued -> ModelPackStatus.Downloading(
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = config.sizeBytes,
            packId = config.packId
        )

        is ModelPackWorkState.Downloading -> ModelPackStatus.Downloading(
            progressPercent = workState.progressPercent,
            downloadedBytes = workState.downloadedBytes,
            totalBytes = workState.totalBytes.takeIf { it > 0L } ?: config.sizeBytes,
            packId = config.packId
        )

        is ModelPackWorkState.Verifying -> ModelPackStatus.Verifying(
            version = workState.version.ifBlank { config.version },
            packId = config.packId
        )

        is ModelPackWorkState.Failed -> ModelPackStatus.Error(
            reason = workState.reason,
            canRetry = true,
            packId = config.packId
        )

        ModelPackWorkState.Idle,
        is ModelPackWorkState.Succeeded,
        ModelPackWorkState.Cancelled -> null
    }

    private fun resolveSettledStatus(
        catalog: ModelPackCatalog,
        evaluated: List<EvaluatedCatalogPack>,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        runtimeState: LiteRtDynamicRuntimeState
    ): ModelPackStatus {
        if (isInstalledValid && installed != null) {
            if (runtimeState !is LiteRtDynamicRuntimeState.Installed) {
                return ModelPackStatus.InstallingRuntime(
                    progressPercent = null,
                    packId = installed.packId
                )
            }
            val matchingConfig = catalog.findById(installed.packId)
            return ModelPackStatus.Installed(
                version = installed.version,
                sizeBytes = installed.sizeBytes,
                isUpdateAvailable = matchingConfig != null &&
                    matchingConfig.isEnabled &&
                    installed.version != matchingConfig.version,
                packId = installed.packId
            )
        }

        val recommended = evaluated.firstOrNull { it.isRecommended && it.compatibility.isCompatible }
        if (recommended != null) {
            return ModelPackStatus.NotInstalled(
                availableVersion = recommended.config.version,
                downloadSizeBytes = recommended.config.sizeBytes,
                packId = recommended.config.packId
            )
        }

        val compatibility = compatibilityProvider.selectAggregateCompatibility(evaluated, installedPackId = null)
        return ModelPackStatus.Incompatible(
            reason = compatibility.reason ?: ModelPackIncompatibilityReason.PACK_DISABLED,
            requiredRamMb = compatibility.requiredRamMb,
            availableRamMb = compatibility.availableRamMb,
            requiredStorageBytes = compatibility.requiredStorageBytes,
            availableStorageBytes = compatibility.availableStorageBytes,
            packId = ""
        )
    }

    private fun resolveWorkTargetConfig(catalog: ModelPackCatalog, packId: String): ModelPackConfig? =
        packId.takeIf { it.isNotBlank() }?.let { catalog.findById(it) }

    private fun ModelPackWorkState.isActiveDownload(): Boolean =
        this is ModelPackWorkState.Queued ||
            this is ModelPackWorkState.Downloading ||
            this is ModelPackWorkState.Verifying

    private fun ModelPackWorkState.targetPackId(): String? = when (this) {
        is ModelPackWorkState.Queued -> packId.takeIf { it.isNotBlank() }
        is ModelPackWorkState.Downloading -> packId.takeIf { it.isNotBlank() }
        is ModelPackWorkState.Verifying -> packId.takeIf { it.isNotBlank() }
        is ModelPackWorkState.Failed -> packId.takeIf { it.isNotBlank() }
        is ModelPackWorkState.Succeeded -> packId.takeIf { it.isNotBlank() }
        ModelPackWorkState.Idle,
        ModelPackWorkState.Cancelled -> null
    }
}
