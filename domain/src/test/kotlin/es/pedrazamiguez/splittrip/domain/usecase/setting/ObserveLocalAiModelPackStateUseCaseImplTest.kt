package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.ObserveLocalAiModelPackStateUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ObserveLocalAiModelPackStateUseCaseImplTest {

    private lateinit var localAiModelPackService: LocalAiModelPackService
    private lateinit var featureGateService: FeatureGateService
    private lateinit var aiModelResolverService: AiModelResolverService
    private lateinit var useCase: ObserveLocalAiModelPackStateUseCaseImpl

    private val statusFlow = MutableStateFlow<ModelPackStatus>(ModelPackStatus.NotInstalled("1.0.0", 500_000_000L))
    private val catalogEntriesFlow = MutableStateFlow<List<ModelPackCatalogEntry>>(emptyList())
    private val entitledFlow = MutableStateFlow(true)
    private val wifiOnlyFlow = MutableStateFlow(true)
    private val activeEngineFlow = MutableStateFlow(AiEngineType.AI_CORE_GEMMA_4)

    private val compatibleResult = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 4096,
        availableStorageBytes = 4_000_000_000L,
        requiredStorageBytes = 1_073_741_824L,
        supportedAbis = listOf("arm64-v8a")
    )

    @BeforeEach
    fun setUp() {
        localAiModelPackService = mockk()
        featureGateService = mockk()
        aiModelResolverService = mockk()

        every { localAiModelPackService.observeStatus() } returns statusFlow
        every { localAiModelPackService.observeCatalogEntries() } returns catalogEntriesFlow
        every { localAiModelPackService.observeWifiOnlyPreference() } returns wifiOnlyFlow
        every { localAiModelPackService.checkDeviceCompatibility() } returns compatibleResult
        every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns entitledFlow
        every { aiModelResolverService.getActiveModel() } returns activeEngineFlow
        every { aiModelResolverService.isAiCoreSupported() } returns false

        useCase = ObserveLocalAiModelPackStateUseCaseImpl(
            localAiModelPackService = localAiModelPackService,
            featureGateService = featureGateService,
            aiModelResolverService = aiModelResolverService
        )
    }

    @Test
    fun `combines status, entitlement, wifiOnly, compatibility, and active engine into LocalAiModelPackState`() =
        runTest {
            val state = useCase().first()

            assertEquals(ModelPackStatus.NotInstalled("1.0.0", 500_000_000L), state.status)
            assertTrue(state.isEntitled)
            assertTrue(state.isWifiOnly)
            assertEquals(compatibleResult, state.compatibility)
            assertEquals(AiEngineType.AI_CORE_GEMMA_4, state.activeEngine)
            assertFalse(state.isAiCoreSupported)
        }

    @Test
    fun `ObserveLocalAiModelPackStateUseCaseImpl combines catalogEntries, recommendedPackId, and installedPackId`() =
        runTest {
            val compactEntry = ModelPackCatalogEntry(
                config = ModelPackConfig(
                    packId = "qwen3-0.6b-litert-v1",
                    displayName = "Compact",
                    tier = ModelPackTier.COMPACT,
                    priority = 10,
                    version = "1.0.0",
                    downloadUrl = "https://models.splittrip.eu/compact.litertlm",
                    sha256Checksum = "a".repeat(64),
                    sizeBytes = 343_490_560L,
                    minRamMb = 2048,
                    minFreeStorageBytes = 1_073_741_824L,
                    isEnabled = true
                ),
                compatibility = compatibleResult,
                status = ModelPackStatus.Installed("1.0.0", 343_490_560L, packId = "qwen3-0.6b-litert-v1"),
                isRecommended = false,
                isInstalled = true,
                isActiveDownload = false
            )
            val balancedEntry = ModelPackCatalogEntry(
                config = ModelPackConfig(
                    packId = "qwen2.5-1.5b-litert-v1",
                    displayName = "Balanced",
                    tier = ModelPackTier.BALANCED,
                    priority = 20,
                    version = "1.0.0",
                    downloadUrl = "https://models.splittrip.eu/balanced.litertlm",
                    sha256Checksum = "b".repeat(64),
                    sizeBytes = 1_597_922_856L,
                    minRamMb = 3072,
                    minFreeStorageBytes = 3_221_225_472L,
                    isEnabled = true
                ),
                compatibility = compatibleResult,
                status = ModelPackStatus.NotInstalled("1.0.0", 1_597_922_856L, packId = "qwen2.5-1.5b-litert-v1"),
                isRecommended = true,
                isInstalled = false,
                isActiveDownload = false
            )
            catalogEntriesFlow.value = listOf(compactEntry, balancedEntry)
            statusFlow.value = compactEntry.status

            val state = useCase().first()

            assertEquals(listOf(compactEntry, balancedEntry), state.catalogEntries)
            assertEquals("qwen2.5-1.5b-litert-v1", state.recommendedPackId)
            assertEquals("qwen3-0.6b-litert-v1", state.installedPackId)
        }

    @Test
    fun `isOptInBannerEligible is true when at least one catalog pack is compatible even if larger tiers fail`() =
        runTest {
            val incompatibleResult = compatibleResult.copy(
                isCompatible = false,
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
            )
            every { localAiModelPackService.checkDeviceCompatibility() } returns incompatibleResult
            val compactCompatible = ModelPackCatalogEntry(
                config = ModelPackConfig(
                    packId = "qwen3-0.6b-litert-v1",
                    tier = ModelPackTier.COMPACT,
                    priority = 10,
                    version = "1.0.0",
                    downloadUrl = "https://models.splittrip.eu/compact.litertlm",
                    sha256Checksum = "a".repeat(64),
                    sizeBytes = 343_490_560L,
                    minRamMb = 2048,
                    minFreeStorageBytes = 1_073_741_824L,
                    isEnabled = true
                ),
                compatibility = compatibleResult,
                status = ModelPackStatus.NotInstalled("1.0.0", 343_490_560L, packId = "qwen3-0.6b-litert-v1"),
                isRecommended = true,
                isInstalled = false,
                isActiveDownload = false
            )
            val highIncompatible = compactCompatible.copy(
                config = compactCompatible.config.copy(
                    packId = "gemma4-e2b-litert-v1",
                    tier = ModelPackTier.HIGH_QUALITY,
                    priority = 30
                ),
                compatibility = incompatibleResult,
                status = ModelPackStatus.Incompatible(
                    reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                    requiredRamMb = 6144,
                    availableRamMb = 2048L,
                    requiredStorageBytes = 5_368_709_120L,
                    availableStorageBytes = 8_000_000_000L,
                    packId = "gemma4-e2b-litert-v1"
                ),
                isRecommended = false
            )
            catalogEntriesFlow.value = listOf(compactCompatible, highIncompatible)

            val state = useCase().first()

            assertTrue(state.hasCompatiblePack)
            assertTrue(state.isOptInBannerEligible)
        }

    @Test
    fun `isOptInBannerEligible is false when all catalog packs are incompatible`() = runTest {
        val incompatibleResult = compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
        )
        val incompatibleEntry = ModelPackCatalogEntry(
            config = ModelPackConfig(
                packId = "qwen3-0.6b-litert-v1",
                tier = ModelPackTier.COMPACT,
                priority = 10,
                version = "1.0.0",
                downloadUrl = "https://models.splittrip.eu/compact.litertlm",
                sha256Checksum = "a".repeat(64),
                sizeBytes = 343_490_560L,
                minRamMb = 2048,
                minFreeStorageBytes = 1_073_741_824L,
                isEnabled = true
            ),
            compatibility = incompatibleResult,
            status = ModelPackStatus.Incompatible(
                reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM,
                requiredRamMb = 2048,
                availableRamMb = 1024L,
                requiredStorageBytes = 1_073_741_824L,
                availableStorageBytes = 4_000_000_000L,
                packId = "qwen3-0.6b-litert-v1"
            ),
            isRecommended = false,
            isInstalled = false,
            isActiveDownload = false
        )
        catalogEntriesFlow.value = listOf(incompatibleEntry)

        val state = useCase().first()

        assertFalse(state.hasCompatiblePack)
        assertFalse(state.isOptInBannerEligible)
    }

    @Test
    fun `isOptInBannerEligible is true only when entitled, compatible, and NotInstalled`() = runTest {
        val state = useCase().first()

        assertTrue(state.isOptInBannerEligible)
    }

    @Test
    fun `isOptInBannerEligible is false when Free user, incompatible device, or already Installed`() = runTest {
        entitledFlow.value = false
        assertFalse(useCase().first().isOptInBannerEligible)

        entitledFlow.value = true
        every { localAiModelPackService.checkDeviceCompatibility() } returns compatibleResult.copy(
            isCompatible = false,
            reason = ModelPackIncompatibilityReason.INSUFFICIENT_RAM
        )
        assertFalse(useCase().first().isOptInBannerEligible)

        every { localAiModelPackService.checkDeviceCompatibility() } returns compatibleResult
        statusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
        assertFalse(useCase().first().isOptInBannerEligible)
    }

    @Test
    fun `isOptInBannerEligible is false when AICore is supported`() = runTest {
        every { aiModelResolverService.isAiCoreSupported() } returns true

        assertFalse(useCase().first().isOptInBannerEligible)
    }
}
