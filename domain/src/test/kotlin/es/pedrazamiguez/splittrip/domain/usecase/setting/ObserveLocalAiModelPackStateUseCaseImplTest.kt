package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ModelPackIncompatibilityReason
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
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
