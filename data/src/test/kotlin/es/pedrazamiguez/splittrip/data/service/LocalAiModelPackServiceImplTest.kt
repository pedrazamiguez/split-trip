package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.repository.impl.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.worker.LocalAiModelPackWorkScheduler
import es.pedrazamiguez.splittrip.data.worker.ModelPackWorkState
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
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
    private var isEngineInitialized = false

    private val configFlow = MutableStateFlow(
        ModelPackConfig(
            packId = "gemma-litert-receipt-v1",
            version = "1.0.0",
            downloadUrl = "https://example.com/model.litertlm",
            sha256Checksum = "abcdef123456",
            sizeBytes = 500_000_000L,
            minRamMb = 4096,
            minFreeStorageBytes = 1_073_741_824L,
            isEnabled = true
        )
    )
    private val entitledFlow = MutableStateFlow(true)
    private val installedFlow = MutableStateFlow<InstalledModelPackMetadata?>(null)
    private val wifiOnlyFlow = MutableStateFlow(true)
    private val workStateFlow = MutableStateFlow<ModelPackWorkState>(ModelPackWorkState.Idle)

    private val compatibleResult = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 4096,
        availableStorageBytes = 4_000_000_000L,
        requiredStorageBytes = 1_073_741_824L,
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
        isEngineInitialized = false

        every { appConfigRepository.localAiModelPackConfig } returns configFlow
        every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns entitledFlow
        every { preferences.installedPackMetadata } returns installedFlow
        every { preferences.wifiOnlyDownload } returns wifiOnlyFlow
        every { workScheduler.observeWorkState() } returns workStateFlow
        every { compatibilityProvider.evaluate(any(), any()) } returns compatibleResult
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
            ioDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        service.close()
    }

    @Test
    fun `observeStatus emits NotInstalled when compatible and no file installed`() = runTest(testDispatcher) {
        val status = service.observeStatus().first()

        assertEquals(ModelPackStatus.NotInstalled("1.0.0", 500_000_000L), status)
    }

    @Test
    fun `observeStatus emits Downloading with progress when worker reports Downloading`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Downloading(
            progressPercent = 45,
            downloadedBytes = 225_000_000L,
            totalBytes = 500_000_000L
        )

        val status = service.observeStatus().first()

        assertEquals(
            ModelPackStatus.Downloading(45, 225_000_000L, 500_000_000L),
            status
        )
    }

    @Test
    fun `observeStatus emits Verifying when worker reports Verifying`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Verifying("1.0.0")

        val status = service.observeStatus().first()

        assertEquals(ModelPackStatus.Verifying("1.0.0"), status)
    }

    @Test
    fun `observeStatus emits Installed when metadata and valid file exist`() = runTest(testDispatcher) {
        installedFlow.value = InstalledModelPackMetadata(
            version = "1.0.0",
            sizeBytes = 500_000_000L,
            sha256Checksum = "abcdef123456",
            filePath = "/data/model.litertlm"
        )
        every { storageManager.isFileValid("/data/model.litertlm", 500_000_000L) } returns true

        val status = service.observeStatus().first()

        assertEquals(
            ModelPackStatus.Installed(version = "1.0.0", sizeBytes = 500_000_000L, isUpdateAvailable = false),
            status
        )
    }

    @Test
    fun `observeStatus emits Installed with isUpdateAvailable true when remote version differs`() =
        runTest(testDispatcher) {
            installedFlow.value = InstalledModelPackMetadata(
                version = "0.9.0",
                sizeBytes = 400_000_000L,
                sha256Checksum = "old",
                filePath = "/data/model.litertlm"
            )
            every { storageManager.isFileValid("/data/model.litertlm", 400_000_000L) } returns true

            val status = service.observeStatus().first()

            assertEquals(
                ModelPackStatus.Installed(version = "0.9.0", sizeBytes = 400_000_000L, isUpdateAvailable = true),
                status
            )
        }

    @Test
    fun `observeStatus emits Incompatible when device fails compatibility check`() = runTest(testDispatcher) {
        every { compatibilityProvider.evaluate(any(), true) } returns compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
            availableRamMb = 2048L
        )

        val status = service.observeStatus().first()

        assertTrue(status is ModelPackStatus.Incompatible)
        assertEquals(ModelPackIncompatibilityReason.INSUFFICIENT_RAM, (status as ModelPackStatus.Incompatible).reason)
    }

    @Test
    fun `observeStatus emits Error with reason when worker reports Failed`() = runTest(testDispatcher) {
        workStateFlow.value = ModelPackWorkState.Failed(ModelPackErrorReason.CHECKSUM_MISMATCH)

        val status = service.observeStatus().first()

        assertEquals(ModelPackStatus.Error(ModelPackErrorReason.CHECKSUM_MISMATCH), status)
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
    fun `requestDownload fails when remote config URL is blank or non-HTTPS`() = runTest(testDispatcher) {
        configFlow.value = configFlow.value.copy(downloadUrl = "")

        val resultBlank = service.requestDownload()
        assertTrue(resultBlank.isFailure)

        configFlow.value = configFlow.value.copy(downloadUrl = "http://insecure.example.com/model.litertlm")
        val resultHttp = service.requestDownload()
        assertTrue(resultHttp.isFailure)
        verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
    }

    @Test
    fun `requestDownload fails when device is incompatible`() = runTest(testDispatcher) {
        every { compatibilityProvider.evaluate(any(), any()) } returns compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.UNSUPPORTED_ABI
        )

        val result = service.requestDownload()

        assertTrue(result.isFailure)
        verify(exactly = 0) { workScheduler.enqueueDownload(any(), any()) }
    }

    @Test
    fun `requestDownload enqueues WorkManager download respecting wifiOnly preference when entitled and compatible`() =
        runTest(testDispatcher) {
            wifiOnlyFlow.value = false

            val result = service.requestDownload()

            assertTrue(result.isSuccess)
            verify(exactly = 1) { workScheduler.enqueueDownload(configFlow.value, false) }
        }

    @Test
    fun `cancelDownload cancels work scheduler and cleans temp files`() = runTest(testDispatcher) {
        val result = service.cancelDownload()

        assertTrue(result.isSuccess)
        verify(exactly = 1) { workScheduler.cancelDownload() }
        verify(exactly = 1) { storageManager.deleteTempFiles("gemma-litert-receipt-v1", "1.0.0") }
    }

    @Test
    fun `deletePack cancels work, closes engine, deletes files, and clears metadata`() =
        runTest(testDispatcher) {
            installedFlow.value = InstalledModelPackMetadata(
                version = "1.0.0",
                sizeBytes = 500_000_000L,
                sha256Checksum = "abcdef123456",
                filePath = "/data/model.litertlm"
            )
            every { storageManager.isFileValid("/data/model.litertlm", 500_000_000L) } returns true
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
            version = "1.0.0",
            sizeBytes = 500_000_000L,
            sha256Checksum = "abcdef123456",
            filePath = "/data/missing.litertlm"
        )
        every { storageManager.isFileValid("/data/missing.litertlm", 500_000_000L) } returns false
        advanceUntilIdle()

        assertFalse(service.isPackInstalled())
        assertNull(service.getInstalledModelPath())
    }
}
