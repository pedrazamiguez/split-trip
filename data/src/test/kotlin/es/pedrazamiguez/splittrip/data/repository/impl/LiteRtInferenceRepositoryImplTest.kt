package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LiteRtInferenceRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var modelPackService: LocalAiModelPackService
    private lateinit var featureGateService: FeatureGateService
    private lateinit var engineClient: LiteRtEngineClient
    private lateinit var repository: LiteRtInferenceRepositoryImpl

    @BeforeEach
    fun setUp() {
        modelPackService = mockk()
        featureGateService = mockk()
        engineClient = mockk()

        every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns flowOf(true)
        every { modelPackService.getInstalledModelPath() } returns "/data/ai_models/model.litertlm"

        repository = LiteRtInferenceRepositoryImpl(
            modelPackService = modelPackService,
            featureGateService = featureGateService,
            engineClient = engineClient,
            defaultDispatcher = testDispatcher
        )
    }

    @Test
    fun `getEngineType returns LITE_RT_LM`() {
        assertEquals(AiEngineType.LITE_RT_LM, repository.getEngineType())
    }

    @Test
    fun `generateContent succeeds with engine output when entitled and pack installed`() = runTest(testDispatcher) {
        coEvery {
            engineClient.generate(
                modelPath = "/data/ai_models/model.litertlm",
                prompt = "Receipt text",
                systemInstruction = null
            )
        } returns """{"amount":"42.00"}"""

        val result = repository.generateContent("Receipt text")

        assertTrue(result.isSuccess)
        assertEquals("""{"amount":"42.00"}""", result.getOrThrow())
    }

    @Test
    fun `generateContent fails with LocalAiModelNotEntitledException when user is not entitled`() =
        runTest(testDispatcher) {
            every { featureGateService.isFeatureEnabled(GatedFeature.LOCAL_AI_MODEL_PACK) } returns flowOf(false)

            val result = repository.generateContent("Receipt text")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is LocalAiModelNotEntitledException)
        }

    @Test
    fun `generateContent fails with LocalAiModelUnavailableException when pack is not installed`() =
        runTest(testDispatcher) {
            every { modelPackService.getInstalledModelPath() } returns null

            val result = repository.generateContent("Receipt text")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is LocalAiModelUnavailableException)
        }

    @Test
    fun `generateStructuredOutput passes JSON schema system instruction and sanitizes fenced JSON output`() =
        runTest(testDispatcher) {
            val systemSlot = slot<String>()
            coEvery {
                engineClient.generate(
                    modelPath = "/data/ai_models/model.litertlm",
                    prompt = "Extract",
                    systemInstruction = capture(systemSlot)
                )
            } returns "```json\n{\"amount\":\"15.50\",\"currency\":\"EUR\"}\n```"

            val result = repository.generateStructuredOutput("Extract", """{"type":"object"}""")

            assertTrue(result.isSuccess)
            assertTrue(systemSlot.captured.contains("""{"type":"object"}"""))
            assertEquals("""{"amount":"15.50","currency":"EUR"}""", result.getOrThrow())
        }

    @Test
    fun `generateStructuredOutput wraps engine exception into Result failure`() = runTest(testDispatcher) {
        coEvery {
            engineClient.generate(any(), any(), any())
        } throws IllegalStateException("Native failure")

        val result = repository.generateStructuredOutput("Extract", "{}")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocalAiModelUnavailableException)
    }
}
