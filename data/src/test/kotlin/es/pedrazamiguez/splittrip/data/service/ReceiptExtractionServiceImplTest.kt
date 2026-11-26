package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptExtractionServiceImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var aiCoreInferenceRepository: AiInferenceRepository
    private lateinit var liteRtInferenceRepository: AiInferenceRepository
    private lateinit var aiModelResolver: AiModelResolverService
    private lateinit var localAiModelPackService: LocalAiModelPackService
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
        every { resources.openRawResource(any()) } answers { ByteArrayInputStream("Custom prompt %1\$s".toByteArray()) }

        aiCoreCapabilityProvider = mockk()
        aiCoreInferenceRepository = mockk()
        liteRtInferenceRepository = mockk()
        aiModelResolver = mockk()
        localAiModelPackService = mockk()
        appConfigRepository = mockk()

        every { localAiModelPackService.isPackInstalled() } returns false

        val blacklistFlow = MutableStateFlow(listOf("razor", "private", "toothbrushes"))
        every { appConfigRepository.ocrSafetyFalsePositivesBlacklist } returns blacklistFlow

        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { aiModelResolver.getDeveloperOverrideModel() } returns flowOf(null)

        service = ReceiptExtractionServiceImpl(
            context = context,
            appConfigRepository = appConfigRepository,
            aiCoreCapabilityProvider = aiCoreCapabilityProvider,
            aiCoreInferenceRepository = lazy { aiCoreInferenceRepository },
            liteRtInferenceRepository = lazy { liteRtInferenceRepository },
            aiModelResolver = aiModelResolver,
            localAiModelPackService = localAiModelPackService,
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
        every { localAiModelPackService.isPackInstalled() } returns true
        every { aiCoreCapabilityProvider.isSupported() } returns false
        advanceUntilIdle()

        val capability = service.capability()

        assertEquals(ExtractionCapability.ON_DEVICE_AI, capability)
    }

    @Test
    fun `capability returns UNSUPPORTED when LITE_RT_LM active, pack missing, AICore unsupported`() =
        runTest(testDispatcher) {
            activeEngineFlow.value = AiEngineType.LITE_RT_LM
            every { localAiModelPackService.isPackInstalled() } returns false
            every { aiCoreCapabilityProvider.isSupported() } returns false
            advanceUntilIdle()

            val capability = service.capability()

            assertEquals(ExtractionCapability.UNSUPPORTED, capability)
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
        every { localAiModelPackService.isPackInstalled() } returns true
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
    fun `extract returns failure when inference fails`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns
            Result.failure(Exception("Inference failed"))
        advanceUntilIdle()

        val result = service.extract(rawReceiptText)

        assertTrue(result.isFailure)
    }

    @Test
    fun `extract retries with aggressive truncation when initial inference fails`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val longText = "A".repeat(2000)
        val dirtyText = RawReceiptText(
            fullText = longText,
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )

        val promptSlot = mutableListOf<String>()
        coEvery { aiCoreInferenceRepository.generateContent(capture(promptSlot)) } returnsMany listOf(
            Result.failure(Exception("Inference failed first time")),
            Result.success("{}")
        )

        val result = service.extract(dirtyText)

        assertTrue(result.isSuccess)
        assertEquals(2, promptSlot.size)
        assertTrue(promptSlot[0].length > promptSlot[1].length)
        assertTrue(promptSlot[1].contains("A".repeat(1000)))
        assertFalse(promptSlot[1].contains("A".repeat(1001)))
    }

    @Test
    fun `extract routes to override engine when explicit engineType is passed`() = runTest(
        testDispatcher
    ) {
        // Active model resolves to AI_CORE but we pass LITE_RT_LM override
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { localAiModelPackService.isPackInstalled() } returns true

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
        // Verify the updated CRITICAL instruction referencing the arrays
        assertTrue(prompt.contains("extract ALL of these individual totals into the \"amounts\" array"))
        assertTrue(prompt.contains("IVA/tax values shown next to a TOTAL are already included in that TOTAL"))
        // Verify TRAINLINE example uses London-Paris (not Madrid/Barcelona which appear in real Renfe tickets)
        assertTrue(prompt.contains("TRAINLINE London to Paris"))
        assertTrue(prompt.contains("\"title\":\"Train London-Paris\""))
        assertTrue(prompt.contains("\"amounts\":[\"55.00\",\"55.00\"]"))
        // Verify the Renfe example shows the array extraction
        assertTrue(prompt.contains("\"amounts\":[\"42.20\",\"42.20\"]"))
        assertTrue(prompt.contains("\"title\":\"Train Seville-Madrid\""))
        assertTrue(prompt.trimEnd().endsWith("Output:"))
    }

    @Test
    fun `extract sums multiple amounts correctly`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val rawJsonResponse = """
            {
                "amounts": ["55.00", "55.00"],
                "currency": "EUR",
                "date": "2026-05-20",
                "title": "Store"
            }
        """.trimIndent()

        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns Result.success(rawJsonResponse)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("110.00"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 20), receipt.date)
        assertEquals("Store", receipt.title)
    }

    @Test
    fun `extract sanitizes false positive words based on remote config blacklist`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val dirtyText = RawReceiptText(
            fullText = "Private Store Name\nTotal: 12.34 EUR\nRazor\nDate: 2026-05-20",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )

        val promptSlot = io.mockk.slot<String>()
        coEvery { aiCoreInferenceRepository.generateContent(capture(promptSlot)) } returns Result.success("{}")

        service.extract(dirtyText)

        val prompt = promptSlot.captured
        // Verify that the blacklisted words are completely removed from the prompt input
        assertTrue(prompt.contains("Store Name"))
        assertFalse(prompt.contains("Private", ignoreCase = true))
        assertFalse(prompt.contains("Razor", ignoreCase = true))
    }

    @Test
    fun `parses HOME category from AI JSON output`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val rawJsonResponse = """
            {
                "amounts": ["85.00"],
                "currency": "EUR",
                "date": "2026-05-20",
                "title": "Electricity bill",
                "category": "HOME"
            }
        """.trimIndent()
        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns Result.success(rawJsonResponse)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        assertEquals("HOME", result.getOrThrow().category)
    }

    @Test
    fun `parses HEALTH_PERSONAL_CARE category from AI JSON output`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        every { aiCoreCapabilityProvider.isSupported() } returns true
        advanceUntilIdle()

        val rawJsonResponse = """
            {
                "amounts": ["22.50"],
                "currency": "EUR",
                "date": "2026-05-20",
                "title": "Haircut",
                "category": "HEALTH_PERSONAL_CARE"
            }
        """.trimIndent()
        coEvery { aiCoreInferenceRepository.generateContent(any()) } returns Result.success(rawJsonResponse)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        assertEquals("HEALTH_PERSONAL_CARE", result.getOrThrow().category)
    }

    @Test
    fun `extract falls back to AI_CORE_GEMMA_4 when LITE_RT_LM pack fails to load and AICore is supported`() =
        runTest(testDispatcher) {
            activeEngineFlow.value = AiEngineType.LITE_RT_LM
            every { localAiModelPackService.isPackInstalled() } returns true
            every { aiCoreCapabilityProvider.isSupported() } returns true
            advanceUntilIdle()

            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
                Result.failure(LocalAiModelUnavailableException("Failed to load"))
            coEvery { aiCoreInferenceRepository.generateContent(any()) } returns
                Result.success("""{"amounts":["20.00"],"currency":"EUR","date":"2026-05-20","title":"Cafe"}""")

            val result = service.extract(rawReceiptText)

            assertTrue(result.isSuccess)
            val receipt = result.getOrThrow()
            assertEquals(ExtractionSource.AI_CORE, receipt.source)
            assertEquals(BigDecimal("20.00"), receipt.amount)
        }

    @Test
    fun `extract returns NO_OP fallback without error when LITE_RT_LM pack is unavailable and AICore is unsupported`() =
        runTest(testDispatcher) {
            activeEngineFlow.value = AiEngineType.LITE_RT_LM
            every { localAiModelPackService.isPackInstalled() } returns true
            every { aiCoreCapabilityProvider.isSupported() } returns false
            advanceUntilIdle()

            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
                Result.failure(LocalAiModelUnavailableException("Missing"))

            val result = service.extract(rawReceiptText)

            assertTrue(result.isSuccess)
            assertEquals(ExtractionSource.NO_OP, result.getOrThrow().source)
        }

    @Test
    fun `extract sanitizes markdown-fenced JSON returned by LITE_RT_LM`() = runTest(testDispatcher) {
        activeEngineFlow.value = AiEngineType.LITE_RT_LM
        every { localAiModelPackService.isPackInstalled() } returns true
        advanceUntilIdle()

        val fencedJson = "```json\n" +
            "{\"amount\":\"18.00\",\"currency\":\"EUR\",\"date\":\"2026-05-20\",\"title\":\"Lunch\"}\n" +
            "```"
        coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
            Result.success(fencedJson)

        val result = service.extract(rawReceiptText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(ExtractionSource.LITE_RT_LM, receipt.source)
        assertEquals(BigDecimal("18.00"), receipt.amount)
    }
}
