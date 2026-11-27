package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.data.worker.ModelPackWorkState
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList", "TooManyFunctions")
internal class LocalAiModelPackServiceImpl(
    private val appConfigRepository: AppConfigRepository,
    private val featureGateService: FeatureGateService,
    private val compatibilityProvider: LocalAiDeviceCompatibilityProvider,
    private val storageManager: ModelPackStorageManager,
    private val preferences: LocalAiModelPackPreferences,
    private val workScheduler: LocalAiModelPackWorkScheduler,
    private val engineClient: Lazy<LiteRtEngineClient>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalAiModelPackService, AutoCloseable {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + ioDispatcher)

    @Volatile
    private var cachedInstalledPack: InstalledModelPackMetadata? = null

    init {
        serviceScope.launch {
            preferences.installedPackMetadata.collect { metadata ->
                cachedInstalledPack = metadata
            }
        }
    }

    override fun observeStatus(): Flow<ModelPackStatus> = combine(
        appConfigRepository.localAiModelPackCatalog,
        preferences.installedPackMetadata,
        workScheduler.observeWorkState()
    ) { catalog, installed, workState ->
        cachedInstalledPack = installed
        val isInstalledValid = isInstalledPackValid(installed)
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val evaluated = compatibilityProvider.evaluateCatalog(catalog, installedPackId = validInstalledPackId)

        when (workState) {
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
            ModelPackWorkState.Cancelled -> resolveSettledStatus(
                catalog = catalog,
                evaluated = evaluated,
                installed = installed,
                isInstalledValid = isInstalledValid
            )
        }
    }

    override fun observeCatalogEntries(): Flow<List<ModelPackCatalogEntry>> = combine(
        appConfigRepository.localAiModelPackCatalog,
        preferences.installedPackMetadata,
        workScheduler.observeWorkState()
    ) { catalog, installed, workState ->
        cachedInstalledPack = installed
        val isInstalledValid = isInstalledPackValid(installed)
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val evaluated = compatibilityProvider.evaluateCatalog(catalog, installedPackId = validInstalledPackId)

        evaluated.map { entry ->
            mapCatalogEntry(
                entry = entry,
                installed = installed,
                isInstalledValid = isInstalledValid,
                workState = workState
            )
        }
    }

    private fun mapCatalogEntry(
        entry: EvaluatedCatalogPack,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean,
        workState: ModelPackWorkState
    ): ModelPackCatalogEntry {
        val packId = entry.config.packId
        val isThisInstalled = isInstalledValid && installed?.packId == packId
        val isThisActiveWork = workState.targetPackId() == packId
        val status = resolveCatalogEntryStatus(
            entry = entry,
            installed = installed,
            isThisInstalled = isThisInstalled,
            isThisActiveWork = isThisActiveWork,
            workState = workState
        )
        val isActiveDownload = isThisActiveWork && workState.isActiveDownload()

        return ModelPackCatalogEntry(
            config = entry.config,
            compatibility = entry.compatibility,
            status = status,
            isRecommended = entry.isRecommended,
            isInstalled = isThisInstalled,
            isActiveDownload = isActiveDownload
        )
    }

    private fun resolveCatalogEntryStatus(
        entry: EvaluatedCatalogPack,
        installed: InstalledModelPackMetadata?,
        isThisInstalled: Boolean,
        isThisActiveWork: Boolean,
        workState: ModelPackWorkState
    ): ModelPackStatus {
        val packId = entry.config.packId
        if (isThisActiveWork) {
            resolveActiveWorkStatus(entry.config, workState)?.let { return it }
        }
        if (isThisInstalled && installed != null) {
            return ModelPackStatus.Installed(
                version = installed.version,
                sizeBytes = installed.sizeBytes,
                isUpdateAvailable = installed.version != entry.config.version,
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
        isInstalledValid: Boolean
    ): ModelPackStatus {
        if (isInstalledValid && installed != null) {
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

    private suspend fun isInstalledPackValid(installed: InstalledModelPackMetadata?): Boolean {
        if (installed == null) return false
        return withContext(ioDispatcher) {
            storageManager.isFileValid(installed.filePath, installed.sizeBytes)
        }
    }

    override fun checkDeviceCompatibility(): DeviceCompatibilityResult {
        val catalog = appConfigRepository.localAiModelPackCatalog.value
        val validInstalledPackId = cachedInstalledPack?.takeIf {
            storageManager.isFileValid(it.filePath, it.sizeBytes)
        }?.packId
        val evaluated = compatibilityProvider.evaluateCatalog(catalog, installedPackId = validInstalledPackId)
        return compatibilityProvider.selectAggregateCompatibility(evaluated, installedPackId = validInstalledPackId)
    }

    override fun isPackInstalled(): Boolean {
        val cached = cachedInstalledPack ?: return false
        return storageManager.isFileValid(cached.filePath, cached.sizeBytes)
    }

    override fun getInstalledModelPath(): String? {
        val cached = cachedInstalledPack ?: return null
        return if (storageManager.isFileValid(cached.filePath, cached.sizeBytes)) {
            cached.filePath
        } else {
            null
        }
    }

    override fun observeWifiOnlyPreference(): Flow<Boolean> = preferences.wifiOnlyDownload

    override suspend fun setWifiOnlyPreference(wifiOnly: Boolean) {
        preferences.setWifiOnlyDownload(wifiOnly)
    }

    @Suppress("ReturnCount")
    override suspend fun requestDownload(packId: String?): Result<Unit> {
        return try {
            val isEntitled = featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first()
            if (!isEntitled) {
                return Result.failure(LocalAiModelNotEntitledException())
            }

            val catalog = appConfigRepository.localAiModelPackCatalog.value
            val targetConfig = resolveTargetConfigForDownload(catalog, packId)
                ?: return Result.failure(
                    if (packId.isNullOrBlank()) {
                        LocalAiModelUnavailableException("No compatible local AI model pack available")
                    } else {
                        IllegalStateException("Unknown or disabled local AI model pack: $packId")
                    }
                )

            val compatibility = compatibilityProvider.evaluate(
                config = targetConfig,
                requireStorageCheck = true
            )
            if (!compatibility.isCompatible) {
                return Result.failure(
                    LocalAiModelUnavailableException(
                        "Device is incompatible for local AI model pack: ${compatibility.reason}"
                    )
                )
            }

            if (!isValidTargetConfig(targetConfig)) {
                return Result.failure(IllegalStateException("Invalid local AI model pack configuration"))
            }

            val wifiOnly = preferences.wifiOnlyDownload.first()
            workScheduler.enqueueDownload(targetConfig, wifiOnly)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveTargetConfigForDownload(
        catalog: ModelPackCatalog,
        packId: String?
    ): ModelPackConfig? {
        if (!packId.isNullOrBlank()) {
            return catalog.enabledPacks.firstOrNull { it.packId == packId }
        }

        val installed = preferences.installedPackMetadata.first()
        val validInstalled = installed?.takeIf { isInstalledPackValid(it) }
        val installedUpdate = validInstalled?.let { meta ->
            catalog.enabledPacks.firstOrNull { it.packId == meta.packId && it.version != meta.version }
        }
        if (installedUpdate != null) return installedUpdate

        val evaluated = compatibilityProvider.evaluateCatalog(
            catalog = catalog,
            installedPackId = validInstalled?.packId
        )
        return evaluated.firstOrNull { it.isRecommended && it.compatibility.isCompatible }?.config
    }

    override suspend fun cancelDownload(packId: String?): Result<Unit> {
        return try {
            workScheduler.cancelDownload()
            val catalog = appConfigRepository.localAiModelPackCatalog.value
            val targetConfig = packId?.takeIf { it.isNotBlank() }?.let { catalog.findById(it) }
            withContext(ioDispatcher) {
                if (targetConfig != null) {
                    storageManager.deleteTempFiles(targetConfig.packId, targetConfig.version)
                } else {
                    storageManager.deleteAllTempFiles()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePack(): Result<Unit> {
        return try {
            workScheduler.cancelDownload()
            withContext(ioDispatcher) {
                if (engineClient.isInitialized()) {
                    engineClient.value.close()
                }
                storageManager.deleteAllModelFiles()
                preferences.clearInstalledPackMetadata()
                cachedInstalledPack = null
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun close() {
        serviceJob.cancel()
    }

    private fun isValidTargetConfig(config: ModelPackConfig): Boolean {
        val hasRequiredFields = listOf(config.packId, config.version, config.sha256Checksum).none { it.isBlank() }
        return hasRequiredFields && config.downloadUrl.startsWith(HTTPS_SCHEME, ignoreCase = true)
    }

    private companion object {
        private const val HTTPS_SCHEME = "https://"
    }
}
