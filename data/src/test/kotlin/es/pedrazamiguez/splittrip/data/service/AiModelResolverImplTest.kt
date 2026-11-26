package es.pedrazamiguez.splittrip.data.service

import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiModelResolverImplTest {

    private lateinit var userPreferences: UserPreferences
    private lateinit var localAiModelPackService: LocalAiModelPackService
    private lateinit var featureGateService: FeatureGateService
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var resolver: AiModelResolverImpl

    private val packStatusFlow = MutableStateFlow<ModelPackStatus>(
        ModelPackStatus.NotInstalled("1.0.0", 500_000_000L)
    )
    private val entitledFlow = MutableStateFlow(true)

    @BeforeEach
    fun setUp() {
        userPreferences = mockk()
        localAiModelPackService = mockk()
        featureGateService = mockk()
        aiCoreCapabilityProvider = mockk()

        every { userPreferences.activeAiModel } returns flowOf(null)
        every { localAiModelPackService.observeStatus() } returns packStatusFlow
        every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns entitledFlow
        every { aiCoreCapabilityProvider.isSupported() } returns true

        resolver = AiModelResolverImpl(
            userPreferences = userPreferences,
            localAiModelPackService = localAiModelPackService,
            featureGateService = featureGateService,
            aiCoreCapabilityProvider = aiCoreCapabilityProvider
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `getActiveModel returns override value when configured`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.LITE_RT_LM.name)

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.LITE_RT_LM, activeModel)
    }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when override is null`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(null)

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
    }

    @Test
    fun `getActiveModel selects LITE_RT_LM when AICore unsupported, pack Installed, and entitled`() =
        runTest {
            every { aiCoreCapabilityProvider.isSupported() } returns false
            packStatusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
            entitledFlow.value = true

            val activeModel = resolver.getActiveModel().first()

            assertEquals(AiEngineType.LITE_RT_LM, activeModel)
        }

    @Test
    fun `getActiveModel prefers AI_CORE_GEMMA_4 when AICore supported even if pack Installed`() =
        runTest {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            packStatusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
            entitledFlow.value = true

            val activeModel = resolver.getActiveModel().first()

            assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
        }

    @Test
    fun `getActiveModel honors explicit LITE_RT_LM developer override even when AICore is supported`() = runTest {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.LITE_RT_LM.name)

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.LITE_RT_LM, activeModel)
    }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when AICore is unsupported and pack is NotInstalled`() =
        runTest {
            every { aiCoreCapabilityProvider.isSupported() } returns false
            packStatusFlow.value = ModelPackStatus.NotInstalled("1.0.0", 500_000_000L)
            entitledFlow.value = true

            val activeModel = resolver.getActiveModel().first()

            assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
        }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when AICore unsupported and not entitled`() =
        runTest {
            every { aiCoreCapabilityProvider.isSupported() } returns false
            packStatusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
            entitledFlow.value = false

            val activeModel = resolver.getActiveModel().first()

            assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
        }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when pack is Installed but user is not entitled`() = runTest {
        packStatusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
        entitledFlow.value = false

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
    }

    @Test
    fun `getActiveModel falls back to AI_CORE_GEMMA_4 when pack is deleted or NotInstalled`() = runTest {
        packStatusFlow.value = ModelPackStatus.NotInstalled("1.0.0", 500_000_000L)
        entitledFlow.value = true

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
    }

    @Test
    fun `getActiveModel honors explicit AI_CORE_GEMMA_4 developer override even when pack is Installed`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.AI_CORE_GEMMA_4.name)
        packStatusFlow.value = ModelPackStatus.Installed("1.0.0", 500_000_000L)
        entitledFlow.value = true

        val activeModel = resolver.getActiveModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, activeModel)
    }

    @Test
    fun `isAiCoreSupported delegates to AICoreCapabilityProvider`() {
        every { aiCoreCapabilityProvider.isSupported() } returns false
        assertFalse(resolver.isAiCoreSupported())

        every { aiCoreCapabilityProvider.isSupported() } returns true
        assertTrue(resolver.isAiCoreSupported())
    }

    @Test
    fun `getDeveloperOverrideModel returns override value`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(AiEngineType.AI_CORE_GEMMA_4.name)

        val override = resolver.getDeveloperOverrideModel().first()

        assertEquals(AiEngineType.AI_CORE_GEMMA_4, override)
    }

    @Test
    fun `getDeveloperOverrideModel returns null when override is not set`() = runTest {
        every { userPreferences.activeAiModel } returns flowOf(null)

        val override = resolver.getDeveloperOverrideModel().first()

        assertNull(override)
    }

    @Test
    fun `setDeveloperOverrideModel sets value in preferences`() = runTest {
        coEvery { userPreferences.setActiveAiModel(any()) } returns Unit

        resolver.setDeveloperOverrideModel(AiEngineType.LITE_RT_LM)

        coVerify { userPreferences.setActiveAiModel(AiEngineType.LITE_RT_LM.name) }
    }

    @Test
    fun `setDeveloperOverrideModel clears value in preferences when passing null`() = runTest {
        coEvery { userPreferences.setActiveAiModel(any()) } returns Unit

        resolver.setDeveloperOverrideModel(null)

        coVerify { userPreferences.setActiveAiModel(null) }
    }
}
