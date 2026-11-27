package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.litert.LiteRtDynamicRuntimeManager
import es.pedrazamiguez.splittrip.data.litert.LiteRtDynamicRuntimeState
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.data.worker.ModelPackWorkState
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalog
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAiModelPackServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var featureGateService: FeatureGateService
    private lateinit var compatibilityProvider: LocalAiDeviceCompatibilityProvider
    private lateinit var storageManager: ModelPackStorageManager
    private lateinit var preferences: LocalAiModelPackPreferences
    private lateinit var workScheduler: LocalAiModelPackWorkScheduler
    private lateinit var engineClient: LiteRtEngineClient
    private lateinit var dynamicRuntimeManager: LiteRtDynamicRuntimeManager
    private var isEngineInitialized = false

    private val compactConfig = ModelPackConfig(
        packId = "qwen3-0.6b-litert-v1",
        displayName = "Compact (Qwen 3 0.6B)",
        tier = ModelPackTier.COMPACT,
        priority = 10,
        version = "1.0.0",
        downloadUrl = "https://models.splittrip.eu/compact.litertlm",
        sha256Checksum = "a".repeat(64),
        sizeBytes = 343_490_560L,
        minRamMb = 2048,
        minFreeStorageBytes = 1_073_741_824L,
        isEnabled = true
    )

    private val balancedConfig = ModelPackConfig(
        packId = "qwen2.5-1.5b-litert-v1",
        displayName = "Balanced (Qwen 2.5 1.5B)",
        tier = ModelPackTier.BALANCED,
        priority = 20,
        version = "1.0.0",
        downloadUrl = "https://models.splittrip.eu/balanced.litertlm",
        sha256Checksum = "b".repeat(64),
        sizeBytes = 1_597_922_856L,
        minRamMb = 3072,
        minFreeStorageBytes = 3_221_225_472L,
        isEnabled = true
    )

    private val catalogFlow = MutableStateFlow(ModelPackCatalog(packs = listOf(compactConfig, balancedConfig)))
    private val entitledFlow = MutableStateFlow(true)
    private val installedFlow = MutableStateFlow<InstalledModelPackMetadata?>(null)
    private val wifiOnlyFlow = MutableStateFlow(true)
    private val workStateFlow = MutableStateFlow<ModelPackWorkState>(ModelPackWorkState.Idle)
    private val runtimeStateFlow = MutableStateFlow<LiteRtDynamicRuntimeState>(LiteRtDynamicRuntimeState.Installed)

    private val compatibleResult = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 3072,
        availableStorageBytes = 8_000_000_000L,
        requiredStorageBytes = 3_221_225_472L,
        supportedAbis = listOf("arm64-v8a")
    )

    private lateinit var service: LocalAiModelPackServiceImpl

    @BeforeEach
    fun setUp() {
        appConfigRepository = mockk()
        featureGateService = mockk()
        compatibilityProvider = mockk()
        storageManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        workScheduler = mockk(relaxed = true)
        engineClient = mockk(relaxed = true)
        dynamicRuntimeManager = mockk(relaxed = true)
        isEngineInitialized = false

        every { dynamicRuntimeManager.observeState() } returns runtimeStateFlow
        every { dynamicRuntimeManager.isRuntimeInstalled() } returns true
        every { appConfigRepository.localAiModelPackCatalog } returns catalogFlow
        every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns entitledFlow
        every { preferences.installedPackMetadata } returns installedFlow
        every { preferences.wifiOnlyDownload } returns wifiOnlyFlow
        every { workScheduler.observeWorkState() } returns workStateFlow
        every { compatibilityProvider.evaluate(any(), any()) } returns compatibleResult
        every { compatibilityProvider.evaluateCatalog(any(), any()) } answers {
            val catalog = firstArg<ModelPackCatalog>()
            catalog.enabledPacks.mapIndexed { idx, config ->
                EvaluatedCatalogPack(
                    config = config,
                    compatibility = compatibleResult,
                    isRecommended = idx == catalog.enabledPacks.lastIndex
                )
            }
        }
        every { compatibilityProvider.selectAggregateCompatibility(any(), any()) } returns compatibleResult
        every { storageManager.isFileValid(any(), any()) } returns false

        service = LocalAiModelPackServiceImpl(
            appConfigRepository = appConfigRepository,
            featureGateService = featureGateService,
            compatibilityProvider = compatibilityProvider,
            storageManager = storageManager,
            preferences = preferences,
            workScheduler = workScheduler,
            engineClient = object : Lazy<LiteRtEngineClient> {
                override val value: LiteRtEngineClient get() = engineClient
                override fun isInitialized(): Boolean = isEngineInitialized
            },
            dynamicRuntimeManager = dynamicRuntimeManager,
            ioDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        service.close()
    }

    @Test
    fun `observeStatus emits NotInstalled for recommended pack when compatible and no file installed`() =
        runTest(testDispatcher) {
            val status = service.observeStatus().first()

            assertEquals(
                ModelPackStatus.NotInstalled(
                    availableVersion = "1.0.0",
                    downloadSizeBytes = 1_597_922_856L,
                    packId = "qwen2.5-1.5b-litert-v1"
                ),
                status
            )
        }

    @Test
    fun `observeStatus emits Downloading with progress when worker reports Downloading`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Downloading(
            progressPercent = 45,
            downloadedBytes = 225_000_000L,
            totalBytes = 500_000_000L,
            packId = "qwen3-0.6b-litert-v1"
        )

        val status = service.observeStatus().first()

        assertEquals(
            ModelPackStatus.Downloading(45, 225_000_000L, 500_000_000L, packId = "qwen3-0.6b-litert-v1"),
            status
        )
    }

    @Test
    fun `observeStatus emits Verifying when worker reports Verifying`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Verifying("1.0.0", packId = "qwen2.5-1.5b-litert-v1")

        val status = service.observeStatus().first()

        assertEquals(ModelPackStatus.Verifying("1.0.0", packId = "qwen2.5-1.5b-litert-v1"), status)
    }

    @Test
    fun `observeStatus emits Installed when metadata and valid file exist`() = runTest(testDispatcher) {
        installedFlow.value = InstalledModelPackMetadata(
            packId = "qwen2.5-1.5b-litert-v1",
            version = "1.0.0",
            sizeBytes = 1_597_922_856L,
            sha256Checksum = "b".repeat(64),
            filePath = "/data/model.litertlm"
        )
        every { storageManager.isFileValid("/data/model.litertlm", 1_597_922_856L) } returns true

        val status = service.observeStatus().first()

        assertEquals(
            ModelPackStatus.Installed(
                version = "1.0.0",
                sizeBytes = 1_597_922_856L,
                isUpdateAvailable = false,
                packId = "qwen2.5-1.5b-litert-v1"
            ),
            status
        )
    }

    @Test
    fun `observeStatus sets isUpdateAvailable true only when installed packId has newer remote version`() =
        runTest(testDispatcher) {
            installedFlow.value = InstalledModelPackMetadata(
                packId = "qwen3-0.6b-litert-v1",
                version = "0.9.0",
                sizeBytes = 340_000_000L,
                sha256Checksum = "old",
                filePath = "/data/compact.litertlm"
            )
            every { storageManager.isFileValid("/data/compact.litertlm", 340_000_000L) } returns true

            val statusUpdateAvailable = service.observeStatus().first()
            assertEquals(
                ModelPackStatus.Installed(
                    version = "0.9.0",
                    sizeBytes = 340_000_000L,
                    isUpdateAvailable = true,
                    packId = "qwen3-0.6b-litert-v1"
                ),
                statusUpdateAvailable
            )

            installedFlow.value = InstalledModelPackMetadata(
                packId = "qwen2.5-1.5b-litert-v1",
                version = "1.0.0",
                sizeBytes = 1_597_922_856L,
                sha256Checksum = "b".repeat(64),
                filePath = "/data/balanced.litertlm"
            )
            every { storageManager.isFileValid("/data/balanced.litertlm", 1_597_922_856L) } returns true

            val statusUpToDate = service.observeStatus().first()
            assertEquals(
                ModelPackStatus.Installed(
                    version = "1.0.0",
                    sizeBytes = 1_597_922_856L,
                    isUpdateAvailable = false,
                    packId = "qwen2.5-1.5b-litert-v1"
                ),
                statusUpToDate
            )
        }

    @Test
    fun `observeStatus emits Incompatible when device fails compatibility check`() = runTest(testDispatcher) {
        val incompatibleResult = compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            availableRamMb = 1024L,
            requiredRamMb = 2048
        )
        every { compatibilityProvider.evaluateCatalog(any(), any()) } returns listOf(
            EvaluatedCatalogPack(compactConfig, incompatibleResult, isRecommended = false)
        )
        every { compatibilityProvider.selectAggregateCompatibility(any(), any()) } returns incompatibleResult

        val status = service.observeStatus().first()

        assertTrue(status is ModelPackStatus.Incompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_RAM, (status as ModelPackStatus.Incompatible).reason)
    }

    @Test
    fun `observeStatus emits Error with reason when worker reports Failed`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Failed(
            reason = ModelPackErrorReason.CHECKSUM_MISMATCH,
            packId = "qwen3-0.6b-litert-v1"
        )

        val status = service.observeStatus().first()

        assertEquals(
            ModelPackStatus.Error(ModelPackErrorReason.CHECKSUM_MISMATCH, packId = "qwen3-0.6b-litert-v1"),
            status
        )
    }

    @Test
    fun `observeCatalogEntries exposes all enabled packs with individual statuses and recommended flag`() =
        runTest(testDispatcher) {
            val entries = service.observeCatalogEntries().first()

            assertEquals(2, entries.size)
            assertEquals("qwen3-0.6b-litert-v1", entries[0].config.packId)
            assertFalse(entries[0].isRecommended)
            assertTrue(entries[0].status is ModelPackStatus.NotInstalled)
            assertEquals("qwen2.5-1.5b-litert-v1", entries[1].config.packId)
            assertTrue(entries[1].isRecommended)
            assertTrue(entries[1].status is ModelPackStatus.NotInstalled)
        }

    @Test
    fun `observeCatalogEntries marks only targeted pack Downloading while keeping installed pack Installed`() =
        runTest(testDispatcher) {
            installedFlow.value = InstalledModelPackMetadata(
                packId = "qwen3-0.6b-litert-v1",
                version = "1.0.0",
                sizeBytes = 343_490_560L,
                sha256Checksum = "a".repeat(64),
                filePath = "/data/compact.litertlm"
            )
            every { storageManager.isFileValid("/data/compact.litertlm", 343_490_560L) } returns true
            workStateFlow.value = ModelPackWorkState.Downloading(
                progressPercent = 30,
                downloadedBytes = 400_000_000L,
                totalBytes = 1_597_922_856L,
                packId = "qwen2.5-1.5b-litert-v1"
            )

            val entries = service.observeCatalogEntries().first()
            val compact = entries.first { it.config.packId == "qwen3-0.6b-litert-v1" }
            val balanced = entries.first { it.config.packId == "qwen2.5-1.5b-litert-v1" }

            assertTrue(compact.isInstalled)
            assertFalse(compact.isActiveDownload)
            assertTrue(compact.status is ModelPackStatus.Installed)

            assertFalse(balanced.isInstalled)
            assertTrue(balanced.isActiveDownload)
            assertTrue(balanced.status is ModelPackStatus.Downloading)
        }

    @Test
    fun `requestDownload fails with LocalAiModelNotEntitledException when user lacks Pro entitlement`() =
        runTest(testDispatcher) {
            entitledFlow.value = false

            val result = service.requestDownload()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is LocalAiModelNotEntitledException)
            verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
        }

    @Test
    fun `requestDownload with explicit packId enqueues that specific pack when entitled and compatible`() =
        runTest(testDispatcher) {
            wifiOnlyFlow.value = false

            val result = service.requestDownload(packId = "qwen3-0.6b-litert-v1")

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.enqueueDownload(compactConfig, false) }
        }

    @Test
    fun `requestDownload with explicit packId fails when pack is incompatible, disabled, or unknown`() =
        runTest(testDispatcher) {
            val unknownResult = service.requestDownload(packId = "missing-pack")
            assertTrue(unknownResult.isFailure)
            assertTrue(unknownResult.exceptionOrNull() is IllegalStateException)

            catalogFlow.value = ModelPackCatalog(packs = listOf(compactConfig.copy(isEnabled = false), balancedConfig))
            val disabledResult = service.requestDownload(packId = "qwen3-0.6b-litert-v1")
            assertTrue(disabledResult.isFailure)
            assertTrue(disabledResult.exceptionOrNull() is IllegalStateException)

            every { compatibilityProvider.evaluate(balancedConfig, true) } returns compatibleResult.copy(
                isCompatible = false,
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_STORAGE
            )
            val incompatibleResult = service.requestDownload(packId = "qwen2.5-1.5b-litert-v1")
            assertTrue(incompatibleResult.isFailure)
            assertTrue(incompatibleResult.exceptionOrNull() is LocalAiModelUnavailableException)

            verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
        }

    @Test
    fun `requestDownload with null packId selects recommended compatible pack by default`() =
        runTest(testDispatcher) {
            wifiOnlyFlow.value = true

            val result = service.requestDownload(packId = null)

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.enqueueDownload(balancedConfig, true) }
        }

    @Test
    fun `cancelDownload with explicit packId cancels scheduler and deletes that pack temp file`() =
        runTest(testDispatcher) {
            val result = service.cancelDownload(packId = "qwen3-0.6b-litert-v1")

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.cancelDownload() }
            verify(exactly = 1) { storageManager.deleteTempFiles("qwen3-0.6b-litert-v1", "1.0.0") }
            verify(exactly = 0) { storageManager.deleteAllTempFiles() }
        }

    @Test
    fun `cancelDownload with null packId cancels scheduler and deletes all temp files`() =
        runTest(testDispatcher) {
            val result = service.cancelDownload(packId = null)

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.cancelDownload() }
            verify(exactly = 1) { storageManager.deleteAllTempFiles() }
        }

    @Test
    fun `deletePack cancels work, closes engine, deletes files, and clears metadata`() =
        runTest(testDispatcher) {
            installedFlow.value = InstalledModelPackMetadata(
                packId = "qwen2.5-1.5b-litert-v1",
                version = "1.0.0",
                sizeBytes = 1_597_922_856L,
                sha256Checksum = "b".repeat(64),
                filePath = "/data/model.litertlm"
            )
            every { storageManager.isFileValid("/data/model.litertlm", 1_597_922_856L) } returns true
            isEngineInitialized = true
            every { engineClient.close() } just runs
            every { storageManager.deleteAllModelFiles() } returns true
            coEvery { preferences.clearInstalledPackMetadata() } just runs
            advanceUntilIdle()
            assertTrue(service.isPackInstalled())

            val result = service.deletePack()

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.cancelDownload() }
            verify(exactly = 1) { engineClient.close() }
            verify(exactly = 1) { storageManager.deleteAllModelFiles() }
            coVerify(exactly = 1) { preferences.clearInstalledPackMetadata() }
            assertFalse(service.isPackInstalled())
            assertNull(service.getInstalledModelPath())
        }

    @Test
    fun `isPackInstalled returns false when persisted file was externally deleted`() = runTest(testDispatcher) {
        installedFlow.value = InstalledModelPackMetadata(
            packId = "qwen2.5-1.5b-litert-v1",
            version = "1.0.0",
            sizeBytes = 1_597_922_856L,
            sha256Checksum = "b".repeat(64),
            filePath = "/data/missing.litertlm"
        )
        every { storageManager.isFileValid("/data/missing.litertlm", 1_597_922_856L) } returns false
        advanceUntilIdle()

        assertFalse(service.isPackInstalled())
        assertNull(service.getInstalledModelPath())
    }

    @Test
    fun `requestDownload requests dynamic runtime install before enqueuing download when not installed`() =
        runTest(testDispatcher) {
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            wifiOnlyFlow.value = false

            val result = service.requestDownload(packId = "qwen3-0.6b-litert-v1")

            assertTrue(result.isSuccess)
            verify(exactly = 1) { dynamicRuntimeManager.requestInstall() }
            verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
        }

    @Test
    fun `enqueues pending model pack download automatically when dynamic runtime transitions to Installed`() =
        runTest(testDispatcher) {
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            wifiOnlyFlow.value = false

            service.requestDownload(packId = "qwen3-0.6b-litert-v1")
            advanceUntilIdle()
            verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }

            every { dynamicRuntimeManager.isRuntimeInstalled() } returns true
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Installed
            advanceUntilIdle()

            verify(exactly = 1) { workScheduler.enqueueDownload(compactConfig, false) }
        }

    @Test
    fun `observeStatus emits InstallingRuntime while dynamic runtime is Pending, Downloading, or Installing`() =
        runTest(testDispatcher) {
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            service.requestDownload(packId = "qwen3-0.6b-litert-v1")

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Pending
            assertEquals(
                ModelPackStatus.InstallingRuntime(progressPercent = null, packId = "qwen3-0.6b-litert-v1"),
                service.observeStatus().first()
            )

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Downloading(
                bytesDownloaded = 4_000_000L,
                totalBytesToDownload = 10_000_000L
            )
            assertEquals(
                ModelPackStatus.InstallingRuntime(progressPercent = 40, packId = "qwen3-0.6b-litert-v1"),
                service.observeStatus().first()
            )

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Installing
            assertEquals(
                ModelPackStatus.InstallingRuntime(progressPercent = null, packId = "qwen3-0.6b-litert-v1"),
                service.observeStatus().first()
            )
        }

    @Test
    fun `observeStatus emits Error RUNTIME_UNAVAILABLE when dynamic runtime install fails`() =
        runTest(testDispatcher) {
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            service.requestDownload(packId = "qwen3-0.6b-litert-v1")

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = -1
            )

            assertEquals(
                ModelPackStatus.Error(
                    reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                    canRetry = true,
                    packId = "qwen3-0.6b-litert-v1"
                ),
                service.observeStatus().first()
            )
        }

    @Test
    fun `auto-requests dynamic runtime install when valid pack exists on disk and blocks isPackInstalled`() =
        runTest(testDispatcher) {
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            every { storageManager.isFileValid("/data/model.litertlm", 1_597_922_856L) } returns true
            installedFlow.value = InstalledModelPackMetadata(
                packId = "qwen2.5-1.5b-litert-v1",
                version = "1.0.0",
                sizeBytes = 1_597_922_856L,
                sha256Checksum = "b".repeat(64),
                filePath = "/data/model.litertlm"
            )
            advanceUntilIdle()

            verify(atLeast = 1) { dynamicRuntimeManager.requestInstall() }
            assertFalse(service.isPackInstalled())

            every { dynamicRuntimeManager.isRuntimeInstalled() } returns true
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Installed
            advanceUntilIdle()

            assertTrue(service.isPackInstalled())
            assertEquals(
                ModelPackStatus.Installed(
                    version = "1.0.0",
                    sizeBytes = 1_597_922_856L,
                    isUpdateAvailable = false,
                    packId = "qwen2.5-1.5b-litert-v1"
                ),
                service.observeStatus().first()
            )
        }

    @Test
    fun `cancelDownload cancels dynamic runtime install and clears pending download`() = runTest(testDispatcher) {
        every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
        runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
        service.requestDownload(packId = "qwen3-0.6b-litert-v1")

        val cancelResult = service.cancelDownload(packId = "qwen3-0.6b-litert-v1")
        assertTrue(cancelResult.isSuccess)
        verify(exactly = 1) { dynamicRuntimeManager.cancelInstall() }

        runtimeStateFlow.value = LiteRtDynamicRuntimeState.Installed
        advanceUntilIdle()
        verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
    }

    @Test
    fun `deletePack requests deferred uninstall of dynamic runtime module`() = runTest(testDispatcher) {
        val result = service.deletePack()

        assertTrue(result.isSuccess)
        verify(exactly = 1) { dynamicRuntimeManager.cancelInstall() }
        verify(exactly = 1) { dynamicRuntimeManager.requestDeferredUninstall() }
    }

    @Test
    fun `observeStatus and observeCatalogEntries emit Downloading at 0 percent when workState is Queued`() =
        runTest(testDispatcher) {
            workStateFlow.value = ModelPackWorkState.Queued(packId = "qwen3-0.6b-litert-v1")

            val status = service.observeStatus().first()
            val entries = service.observeCatalogEntries().first()

            assertEquals(
                ModelPackStatus.Downloading(
                    progressPercent = 0,
                    downloadedBytes = 0L,
                    totalBytes = compactConfig.sizeBytes,
                    packId = "qwen3-0.6b-litert-v1"
                ),
                status
            )
            assertEquals(status, entries.first { it.config.packId == "qwen3-0.6b-litert-v1" }.status)
        }

    @Test
    fun `observeCatalogEntries maps InstallingRuntime, runtime Failed, Verifying, and Incompatible tiers`() =
        runTest(testDispatcher) {
            val incompatibleResult = compatibleResult.copy(
                isCompatible = false,
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
            )
            every { compatibilityProvider.evaluateCatalog(any(), any()) } returns listOf(
                EvaluatedCatalogPack(compactConfig, compatibleResult, isRecommended = true),
                EvaluatedCatalogPack(balancedConfig, incompatibleResult, isRecommended = false)
            )
            every { dynamicRuntimeManager.isRuntimeInstalled() } returns false
            runtimeStateFlow.value = LiteRtDynamicRuntimeState.NotInstalled
            service.requestDownload(packId = "qwen3-0.6b-litert-v1")

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Downloading(
                bytesDownloaded = 2_500_000L,
                totalBytesToDownload = 10_000_000L
            )
            val installingEntries = service.observeCatalogEntries().first()
            assertEquals(
                ModelPackStatus.InstallingRuntime(progressPercent = 25, packId = "qwen3-0.6b-litert-v1"),
                installingEntries[0].status
            )
            assertTrue(installingEntries[0].isActiveDownload)
            assertTrue(installingEntries[1].status is ModelPackStatus.Incompatible)

            runtimeStateFlow.value = LiteRtDynamicRuntimeState.Failed(
                reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                errorCode = -1
            )
            val failedEntries = service.observeCatalogEntries().first()
            assertEquals(
                ModelPackStatus.Error(
                    reason = ModelPackErrorReason.RUNTIME_UNAVAILABLE,
                    canRetry = true,
                    packId = "qwen3-0.6b-litert-v1"
                ),
                failedEntries[0].status
            )

            workStateFlow.value = ModelPackWorkState.Verifying("1.0.0", packId = "qwen3-0.6b-litert-v1")
            val verifyingEntries = service.observeCatalogEntries().first()
            assertEquals(
                ModelPackStatus.Verifying("1.0.0", packId = "qwen3-0.6b-litert-v1"),
                verifyingEntries[0].status
            )
        }
}
