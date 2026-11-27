package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.litert.LiteRtDynamicRuntimeManager
import es.pedrazamiguez.splittrip.data.litert.LiteRtDynamicRuntimeState
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
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
    private val dynamicRuntimeManager: LiteRtDynamicRuntimeManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalAiModelPackService, AutoCloseable {

    private data class PendingRuntimeDownload(
        val config: ModelPackConfig,
        val wifiOnly: Boolean
    )

    private data class InstalledEntitlementSnapshot(
        val metadata: InstalledModelPackMetadata?,
        val isEntitled: Boolean
    )

    private val statusResolver = ModelPackStatusResolver(compatibilityProvider)
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + ioDispatcher)

    @Volatile
    private var cachedInstalledPack: InstalledModelPackMetadata? = null

    private val pendingRuntimeDownload = MutableStateFlow<PendingRuntimeDownload?>(null)
    private val activeRuntimePackId = MutableStateFlow<String?>(null)

    init {
        serviceScope.launch {
            combine(
                preferences.installedPackMetadata,
                featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK)
            ) { metadata, isEntitled ->
                InstalledEntitlementSnapshot(metadata = metadata, isEntitled = isEntitled)
            }.collect(::handleInstalledPackEntitlement)
        }

        serviceScope.launch {
            dynamicRuntimeManager.observeState().collect(::handleRuntimeStateTransition)
        }
    }

    private suspend fun handleInstalledPackEntitlement(snapshot: InstalledEntitlementSnapshot) {
        val metadata = snapshot.metadata
        cachedInstalledPack = metadata
        val needsRuntimeBackfill = metadata != null &&
            snapshot.isEntitled &&
            isInstalledPackValid(metadata) &&
            !dynamicRuntimeManager.isRuntimeInstalled()
        if (needsRuntimeBackfill &&
            dynamicRuntimeManager.observeState().value is LiteRtDynamicRuntimeState.NotInstalled
        ) {
            activeRuntimePackId.value = metadata.packId
            dynamicRuntimeManager.requestInstall()
        }
    }

    private fun handleRuntimeStateTransition(runtimeState: LiteRtDynamicRuntimeState) {
        when (runtimeState) {
            LiteRtDynamicRuntimeState.Installed -> {
                val pending = pendingRuntimeDownload.getAndUpdate { null }
                if (pending != null) {
                    workScheduler.enqueueDownload(pending.config, pending.wifiOnly)
                }
                activeRuntimePackId.value = null
            }

            is LiteRtDynamicRuntimeState.Failed -> {
                pendingRuntimeDownload.value = null
            }

            LiteRtDynamicRuntimeState.NotInstalled,
            LiteRtDynamicRuntimeState.Pending,
            is LiteRtDynamicRuntimeState.Downloading,
            LiteRtDynamicRuntimeState.Installing -> Unit
        }
    }

    override fun observeStatus(): Flow<ModelPackStatus> = combine(
        appConfigRepository.localAiModelPackCatalog,
        preferences.installedPackMetadata,
        workScheduler.observeWorkState(),
        dynamicRuntimeManager.observeState()
    ) { catalog, installed, workState, runtimeState ->
        cachedInstalledPack = installed
        val isInstalledValid = isInstalledPackValid(installed)
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val evaluated = compatibilityProvider.evaluateCatalog(catalog, installedPackId = validInstalledPackId)

        statusResolver.resolveAggregateStatus(
            catalog = catalog,
            evaluated = evaluated,
            installed = installed,
            isInstalledValid = isInstalledValid,
            workState = workState,
            runtimeState = runtimeState,
            activeRuntimePackId = activeRuntimePackId.value
        )
    }

    override fun observeCatalogEntries(): Flow<List<ModelPackCatalogEntry>> = combine(
        appConfigRepository.localAiModelPackCatalog,
        preferences.installedPackMetadata,
        workScheduler.observeWorkState(),
        dynamicRuntimeManager.observeState()
    ) { catalog, installed, workState, runtimeState ->
        cachedInstalledPack = installed
        val isInstalledValid = isInstalledPackValid(installed)
        val validInstalledPackId = installed?.takeIf { isInstalledValid }?.packId
        val evaluated = compatibilityProvider.evaluateCatalog(catalog, installedPackId = validInstalledPackId)

        evaluated.map { entry ->
            statusResolver.mapCatalogEntry(
                entry = entry,
                installed = installed,
                isInstalledValid = isInstalledValid,
                workState = workState,
                runtimeState = runtimeState,
                activeRuntimePackId = activeRuntimePackId.value
            )
        }
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
        return dynamicRuntimeManager.isRuntimeInstalled() &&
            storageManager.isFileValid(cached.filePath, cached.sizeBytes)
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
            if (dynamicRuntimeManager.isRuntimeInstalled()) {
                pendingRuntimeDownload.value = null
                activeRuntimePackId.value = null
                workScheduler.enqueueDownload(targetConfig, wifiOnly)
            } else {
                pendingRuntimeDownload.value = PendingRuntimeDownload(targetConfig, wifiOnly)
                activeRuntimePackId.value = targetConfig.packId
                dynamicRuntimeManager.requestInstall()
            }
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
            pendingRuntimeDownload.value = null
            activeRuntimePackId.value = null
            dynamicRuntimeManager.cancelInstall()
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
            pendingRuntimeDownload.value = null
            activeRuntimePackId.value = null
            dynamicRuntimeManager.cancelInstall()
            workScheduler.cancelDownload()
            withContext(ioDispatcher) {
                if (engineClient.isInitialized()) {
                    engineClient.value.close()
                }
                storageManager.deleteAllModelFiles()
                preferences.clearInstalledPackMetadata()
                cachedInstalledPack = null
            }
            dynamicRuntimeManager.requestDeferredUninstall()
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
