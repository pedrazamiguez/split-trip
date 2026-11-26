package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.data.worker.ModelPackWorkState
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
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
        appConfigRepository.localAiModelPackConfig,
        preferences.installedPackMetadata,
        workScheduler.observeWorkState()
    ) { config, installed, workState ->
        cachedInstalledPack = installed
        val isInstalledValid = isInstalledPackValid(installed)

        when (workState) {
            ModelPackWorkState.Queued -> ModelPackStatus.Downloading(
                progressPercent = 0,
                downloadedBytes = 0L,
                totalBytes = config.sizeBytes
            )

            is ModelPackWorkState.Downloading -> ModelPackStatus.Downloading(
                progressPercent = workState.progressPercent,
                downloadedBytes = workState.downloadedBytes,
                totalBytes = workState.totalBytes.takeIf { it > 0L } ?: config.sizeBytes
            )

            is ModelPackWorkState.Verifying -> ModelPackStatus.Verifying(
                version = workState.version.ifBlank { config.version }
            )

            is ModelPackWorkState.Failed -> ModelPackStatus.Error(
                reason = workState.reason,
                canRetry = true
            )

            ModelPackWorkState.Idle,
            ModelPackWorkState.Succeeded,
            ModelPackWorkState.Cancelled -> resolveSettledStatus(
                config = config,
                installed = installed,
                isInstalledValid = isInstalledValid
            )
        }
    }

    private fun resolveSettledStatus(
        config: ModelPackConfig,
        installed: InstalledModelPackMetadata?,
        isInstalledValid: Boolean
    ): ModelPackStatus {
        if (isInstalledValid && installed != null) {
            return ModelPackStatus.Installed(
                version = installed.version,
                sizeBytes = installed.sizeBytes,
                isUpdateAvailable = config.isEnabled && installed.version != config.version
            )
        }

        val compatibility = compatibilityProvider.evaluate(config, requireStorageCheck = true)
        val incompatibilityReason = compatibility.reason
        if (!compatibility.isCompatible && incompatibilityReason != null) {
            return ModelPackStatus.Incompatible(
                reason = incompatibilityReason,
                requiredRamMb = compatibility.requiredRamMb,
                availableRamMb = compatibility.availableRamMb,
                requiredStorageBytes = compatibility.requiredStorageBytes,
                availableStorageBytes = compatibility.availableStorageBytes
            )
        }

        return ModelPackStatus.NotInstalled(
            availableVersion = config.version,
            downloadSizeBytes = config.sizeBytes
        )
    }

    private suspend fun isInstalledPackValid(installed: InstalledModelPackMetadata?): Boolean {
        if (installed == null) return false
        return withContext(ioDispatcher) {
            storageManager.isFileValid(installed.filePath, installed.sizeBytes)
        }
    }

    override fun checkDeviceCompatibility(): DeviceCompatibilityResult {
        val config = appConfigRepository.localAiModelPackConfig.value
        return compatibilityProvider.evaluate(
            config = config,
            requireStorageCheck = !isPackInstalled()
        )
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
    override suspend fun requestDownload(): Result<Unit> {
        return try {
            val isEntitled = featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK).first()
            if (!isEntitled) {
                return Result.failure(LocalAiModelNotEntitledException())
            }

            val config = appConfigRepository.localAiModelPackConfig.value
            val compatibility = compatibilityProvider.evaluate(
                config = config,
                requireStorageCheck = !isPackInstalled()
            )
            if (!compatibility.isCompatible) {
                return Result.failure(
                    LocalAiModelUnavailableException(
                        "Device is incompatible for local AI model pack: ${compatibility.reason}"
                    )
                )
            }

            if (
                config.version.isBlank() ||
                !config.downloadUrl.startsWith(HTTPS_SCHEME, ignoreCase = true) ||
                config.sha256Checksum.isBlank()
            ) {
                return Result.failure(IllegalStateException("Invalid local AI model pack configuration"))
            }

            val wifiOnly = preferences.wifiOnlyDownload.first()
            workScheduler.enqueueDownload(config, wifiOnly)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelDownload(): Result<Unit> {
        return try {
            workScheduler.cancelDownload()
            val config = appConfigRepository.localAiModelPackConfig.value
            withContext(ioDispatcher) {
                storageManager.deleteTempFiles(config.packId, config.version)
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

    private companion object {
        private const val HTTPS_SCHEME = "https://"
    }
}
