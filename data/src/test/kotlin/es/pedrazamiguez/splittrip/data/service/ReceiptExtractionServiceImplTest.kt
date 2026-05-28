package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.data.service.ReceiptExtractionServiceImpl.Companion.preComputeTotalIfMultiPassenger
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptExtractionServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var aiCoreInferenceRepository: AiInferenceRepository
    private lateinit var liteRtInferenceRepository: AiInferenceRepository
    private lateinit var aiModelResolver: AiModelResolverService
    private lateinit var service: ReceiptExtractionServiceImpl
    private val activeEngineFlow = MutableStateFlow(AiEngineType.AI_CORE_GEMMA_4)

    private val rawReceiptText = RawReceiptText(
        fullText = "Store Name\nTotal: 12.34 EUR\nDate: 2026-05-20",
        blocks = persistentListOf(),
        recognisedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        val resources = mockk<android.content.res.Resources>(relaxed = true)
        every { context.resources } returns resources
        every { resources.openRawResource(any()) } returns ByteArrayInputStream("Custom prompt %1\$s".toByteArray())

        aiCoreCapabilityProvider = mockk()
        aiCoreInferenceRepository = mockk()
        liteRtInferenceRepository = mockk()
        aiModelResolver = mockk()

        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { aiModelResolver.getDeveloperOverrideModel() } returns flowOf(null)

        service = ReceiptExtractionServiceImpl(
            context = context,
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreInferenceRepository = lazy { aiCoreInferenceRepository },
            liteRtInferenceRepository = lazy { liteRtInferenceRepository },
            aiModelResolver = aiModelResolver,
            defaultDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        service.close()
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `capability returns UNSUPPORTED when AICore capability is not supported`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns false
        advanceUntilIdle()

        val capability = service.capability()
        assertEquals(ExtractionCapability.UNSUPPORTED, capability)
    }

    @Test
    fun `capability returns ON_DEVICE_AI when AICore capability is supported`() = runTest(testDispatcher) {
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val capability = service.capability()
        assertEquals(ExtractionCapability.ON_DEVICE_AI, capability)
    }

    @Test
    fun `capability returns ON_DEVICE_AI when LiteRt is active even if AICore is unsupported`() = runTest(
        testDispatcher
    ) {
        activeEngineFlow.value = AiEngineType.LITE_RT_LM
        every { aiCoreCapabilityProvider.isSupported() } returns false
        advanceUntilIdle()

        val capability = service.capability()

        assertEquals(ExtractionCapability.ON_DEVICE_AI, capability)
    }

    @Test
    fun `extract returns NO_OP fallback immediately when capability is unsupported for AICore`() = runTest(
        testDispatcher
    ) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns false
        advanceUntilIdle()

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.NO_OP, receipt.source)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
        assertNull(receipt.amount)
    }

    @Test
    fun `extract delegates to AICore repository and sanitizes response`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val rawJsonResponse = """
            Here is the response:
            ```json
            {
                "amount": "12.34",
                "currency": "EUR",
                "date": "2026-05-20",
                "title": "Store"
            }
            ```
            Hope it works!
        """.trimIndent()

        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns Result.success(rawJsonResponse)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("12.34"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 20), receipt.date)
        assertEquals("Store", receipt.title)
        assertEquals(ExtractionSource.AI_CORE, receipt.source)
        assertEquals(ExtractionConfidence.HIGH, receipt.confidence)
    }

    @Test
    fun `extract delegates to LiteRt repository and parses cleanly`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.LITE_RT_LM
        advanceUntilIdle()

        val cleanJsonResponse = """
            {
                "amount": "15.50",
                "currency": "USD",
                "date": "2026-05-26",
                "title": "LiteRT Purchase"
            }
        """.trimIndent()

        coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
            Result.success(cleanJsonResponse)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("15.50"), receipt.amount)
        assertEquals("USD", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 26), receipt.date)
        assertEquals("LiteRT Purchase", receipt.title)
        assertEquals(ExtractionSource.LITE_RT_LM, receipt.source)
        assertEquals(ExtractionConfidence.HIGH, receipt.confidence)
    }

    @Test
    fun `extract returns NO_OP fallback when inference fails`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns
            Result.failure(Exception("Inference failed"))
        advanceUntilIdle()

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.NO_OP, receipt.source)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
    }

    @Test
    fun `extract routes to override engine when explicit engineType is passed`() = runTest(
        testDispatcher
    ) {
        // Active model resolves to AI_CORE but we pass LITE_RT_LM override
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow

        val cleanJsonResponse = """
            {
                "amount": "15.50",
                "currency": "USD",
                "date": "2026-05-26",
                "title": "LiteRT Purchase"
            }
        """.trimIndent()

        coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
            Result.success(cleanJsonResponse)

        val result = service.extract(rawReceiptText, engineType = AiEngineType.LITE_RT_LM)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.LITE_RT_LM, receipt.source)
    }

    @Test
    fun `loadPromptTemplate falls back to DEFAULT_PROMPT_TEMPLATE when resources fail`() = runTest(
        testDispatcher
    ) {
        every { context.resources.openRawResource(any()) } throws Exception("Failed to load")
        every { aiCoreCapabilityProvider.isSupported() } returns true

        val promptSlot = io.mockk.slot<String>()
        coEvery { aiCoreInferenceRepository.generateContent(capture(promptSlot)) } returns Result.success("{}")
        advanceUntilIdle()

        service.extract(rawReceiptText)

        val prompt = promptSlot.captured
        // Verify the updated CRITICAL instruction referencing the pre-computed total line
        assertTrue(prompt.contains("Grand total (N tickets): X.XX"))
        assertTrue(prompt.contains("IVA/tax values shown next to a TOTAL are already included in that TOTAL"))
        // Verify TRAINLINE example uses London-Paris (not Madrid/Barcelona which appear in real Renfe tickets)
        assertTrue(prompt.contains("TRAINLINE London to Paris"))
        assertTrue(prompt.contains("\"title\":\"Train London-Paris\""))
        // Verify the Renfe example shows the pre-computed total as first line of input
        assertTrue(prompt.contains("Input: Grand total (2 tickets): 84.40"))
        assertTrue(prompt.contains("\"title\":\"Train Sevilla-Madrid\""))
        // Verify the prompt ends cleanly with "Output:" so the Input->Output pattern is uninterrupted
        assertTrue(prompt.trimEnd().endsWith("Output:"))
    }

    @Test
    fun `preComputeTotalIfMultiPassenger returns unchanged text when only one TOTAL is found`() {
        val singleTotalText = "Shop Receipt\nTOTAL 25.00 EUR\nDate: 2026-01-01"

        val result = preComputeTotalIfMultiPassenger(singleTotalText)

        assertEquals(singleTotalText, result)
    }

    @Test
    fun `preComputeTotalIfMultiPassenger prepends grand total when two per-passenger TOTALs are found`() {
        val twoTicketsText =
            "Ticket A TOTAL 42,20 € IVA 3,84 € Ticket B TOTAL 42,20 € IVA 3,84 €"

        val result = preComputeTotalIfMultiPassenger(twoTicketsText)

        assertTrue(result.startsWith("Grand total (2 tickets): 84.40"))
        assertTrue(result.contains(twoTicketsText))
    }

    @Test
    fun `preComputeTotalIfMultiPassenger handles European decimal format correctly`() {
        val renfeOcrText =
            "RENFE A.PEDRAZA Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA TOTAL 42,20 € " +
                "RENFE A.NARANJO Origen: SEVILLA S JUSTA Destino: MADRID P.ATOCHA TOTAL 42,20 €"

        val result = preComputeTotalIfMultiPassenger(renfeOcrText)

        assertTrue(result.startsWith("Grand total (2 tickets): 84.40"))
    }
}
