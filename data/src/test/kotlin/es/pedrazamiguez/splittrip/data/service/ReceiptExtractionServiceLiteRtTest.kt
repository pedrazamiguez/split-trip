package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.content.res.Resources
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelUnavailableException
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.repository.AiInferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.LocalAiModelPackService
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptExtractionServiceLiteRtTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var appConfigRepository: AppConfigRepository
    private lateinit var aiCoreCapabilityProvider: AICoreCapabilityProvider
    private lateinit var aiCoreInferenceRepository: AiInferenceRepository
    private lateinit var liteRtInferenceRepository: AiInferenceRepository
    private lateinit var aiModelResolver: AiModelResolverService
    private lateinit var localAiModelPackService: LocalAiModelPackService
    private lateinit var service: ReceiptExtractionServiceImpl
    private val activeEngineFlow = MutableStateFlow(AiEngineType.LITE_RT_LM)

    private val rawReceiptText = RawReceiptText(
        fullText = "Store Name\nTotal: 12.34 EUR\nDate: 2026-05-20",
        blocks = persistentListOf(),
        recognisedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        val resources = mockk<Resources>(relaxed = true)
        every { context.resources } returns resources
        every { resources.openRawResource(any()) } answers {
            ByteArrayInputStream("Custom prompt %1\$s".toByteArray())
        }

        aiCoreCapabilityProvider = mockk()
        aiCoreInferenceRepository = mockk()
        liteRtInferenceRepository = mockk()
        aiModelResolver = mockk()
        localAiModelPackService = mockk()
        appConfigRepository = mockk()

        every { localAiModelPackService.isPackInstalled() } returns true
        every { aiCoreCapabilityProvider.isSupported() } returns false
        every { appConfigRepository.ocrSafetyFalsePositivesBlacklist } returns MutableStateFlow(emptyList())

        activeEngineFlow.value = AiEngineType.LITE_RT_LM
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { aiModelResolver.getDeveloperOverrideModel() } returns flowOf(null)

        service = createService()
    }

    @AfterEach
    fun tearDown() {
        service.close()
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `extract enforces timeout for LITE_RT_LM and falls back to AI_CORE_GEMMA_4 without retrying`() =
        runTest(testDispatcher) {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } coAnswers {
                delay(5_000L)
                Result.success("{}")
            }
            coEvery { aiCoreInferenceRepository.generateContent(any()) } returns
                Result.success("""{"amounts":["24.00"],"currency":"EUR","date":"2026-05-20","title":"Dinner"}""")

            val timedService = createService(liteRtInferenceTimeoutMs = 1_000L)
            try {
                advanceUntilIdle()
                val result = timedService.extract(rawReceiptText)

                assertTrue(result.isSuccess)
                assertEquals(ExtractionSource.AI_CORE, result.getOrThrow().source)
                coVerify(exactly = 1) { liteRtInferenceRepository.generateStructuredOutput(any(), any()) }
            } finally {
                timedService.close()
            }
        }

    @Test
    fun `extract enforces timeout for LITE_RT_LM and returns NO_OP fallback when AICore is unsupported`() =
        runTest(testDispatcher) {
            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } coAnswers {
                delay(5_000L)
                Result.success("{}")
            }

            val timedService = createService(liteRtInferenceTimeoutMs = 1_000L)
            try {
                advanceUntilIdle()
                val result = timedService.extract(rawReceiptText)

                assertTrue(result.isSuccess)
                assertEquals(ExtractionSource.NO_OP, result.getOrThrow().source)
                coVerify(exactly = 1) { liteRtInferenceRepository.generateStructuredOutput(any(), any()) }
            } finally {
                timedService.close()
            }
        }

    @Test
    fun `extract returns failure without retry when explicit LITE_RT_LM override times out`() =
        runTest(testDispatcher) {
            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } coAnswers {
                delay(5_000L)
                Result.success("{}")
            }

            val timedService = createService(liteRtInferenceTimeoutMs = 1_000L)
            try {
                advanceUntilIdle()
                val result = timedService.extract(rawReceiptText, engineType = AiEngineType.LITE_RT_LM)

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is LocalAiModelUnavailableException)
                assertTrue(exception?.message?.contains("timed out") == true)
                coVerify(exactly = 1) { liteRtInferenceRepository.generateStructuredOutput(any(), any()) }
            } finally {
                timedService.close()
            }
        }

    @Test
    fun `extract does not retry LITE_RT_LM on non-recoverable LocalAiModelUnavailableException`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            val longReceipt = RawReceiptText(
                fullText = "A".repeat(3_000),
                blocks = persistentListOf(),
                recognisedAt = Instant.now()
            )
            coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
                Result.failure(LocalAiModelUnavailableException("Native init failed"))

            val result = service.extract(longReceipt)

            assertTrue(result.isSuccess)
            assertEquals(ExtractionSource.NO_OP, result.getOrThrow().source)
            coVerify(exactly = 1) { liteRtInferenceRepository.generateStructuredOutput(any(), any()) }
        }

    @Test
    fun `extract does not retry LITE_RT_LM on LocalAiModelNotEntitledException`() = runTest(testDispatcher) {
        val longReceipt = RawReceiptText(
            fullText = "A".repeat(3_000),
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        coEvery { liteRtInferenceRepository.generateStructuredOutput(any(), any()) } returns
            Result.failure(LocalAiModelNotEntitledException())

        val result = service.extract(longReceipt, engineType = AiEngineType.LITE_RT_LM)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocalAiModelNotEntitledException)
        coVerify(exactly = 1) { liteRtInferenceRepository.generateStructuredOutput(any(), any()) }
    }

    @Test
    fun `extract retries LITE_RT_LM once with aggressive truncation when input length overflow occurs`() =
        runTest(testDispatcher) {
            val longReceipt = RawReceiptText(
                fullText = "A".repeat(3_000),
                blocks = persistentListOf(),
                recognisedAt = Instant.now()
            )
            val prompts = mutableListOf<String>()
            coEvery { liteRtInferenceRepository.generateStructuredOutput(capture(prompts), any()) } returnsMany listOf(
                Result.failure(
                    LocalAiModelUnavailableException(
                        "Failed",
                        IllegalArgumentException("Input exceeds max token limit")
                    )
                ),
                Result.success("""{"amounts":["12.00"],"currency":"EUR"}""")
            )

            val result = service.extract(longReceipt, engineType = AiEngineType.LITE_RT_LM)

            assertTrue(result.isSuccess)
            assertEquals(2, prompts.size)
            assertTrue(prompts[1].length < prompts[0].length)
        }

    @Test
    fun `extract uses compact prompt and tighter OCR truncation for LITE_RT_LM without changing AICore prompt`() =
        runTest(testDispatcher) {
            every { aiCoreCapabilityProvider.isSupported() } returns true
            every { context.resources.openRawResource(any()) } throws Exception("Use fallback template")

            val hugeOcr = "H".repeat(2_000) +
                "<receipt_ocr>" +
                "M".repeat(2_000) +
                "</receipt_ocr>" +
                "T".repeat(1_000)
            val hugeReceipt = RawReceiptText(
                fullText = hugeOcr,
                blocks = persistentListOf(),
                recognisedAt = Instant.now()
            )

            val liteRtPromptSlot = slot<String>()
            val aiCorePromptSlot = slot<String>()
            coEvery { liteRtInferenceRepository.generateStructuredOutput(capture(liteRtPromptSlot), any()) } returns
                Result.success("""{"amounts":["10.00"],"currency":"EUR"}""")
            coEvery { aiCoreInferenceRepository.generateContent(capture(aiCorePromptSlot)) } returns
                Result.success("""{"amounts":["10.00"],"currency":"EUR"}""")

            service.extract(hugeReceipt, engineType = AiEngineType.LITE_RT_LM)
            service.extract(hugeReceipt, engineType = AiEngineType.AI_CORE_GEMMA_4)

            val liteRtPrompt = liteRtPromptSlot.captured
            val aiCorePrompt = aiCorePromptSlot.captured
            assertTrue(liteRtPrompt.length < aiCorePrompt.length / 2)
            val inputIndex = liteRtPrompt.lastIndexOf("Input:")
            val openTagCount = Regex("<receipt_ocr>")
                .findAll(liteRtPrompt)
                .count { match -> match.range.first > inputIndex }
            val closeTagCount = Regex("</receipt_ocr>")
                .findAll(liteRtPrompt)
                .count { match -> match.range.first > inputIndex }
            assertEquals(1, openTagCount)
            assertEquals(1, closeTagCount)
            val liteRtOcrPayload = liteRtPrompt
                .substringAfterLast("<receipt_ocr>")
                .substringBeforeLast("</receipt_ocr>")
                .trim()
            assertTrue(liteRtOcrPayload.length <= 2_400)
        }

    private fun createService(
        liteRtInferenceTimeoutMs: Long = ReceiptExtractionServiceImpl.DEFAULT_LITE_RT_INFERENCE_TIMEOUT_MS
    ): ReceiptExtractionServiceImpl = ReceiptExtractionServiceImpl(
        context = context,
        appConfigRepository = appConfigRepository,
        aiCoreCapabilityProvider = aiCoreCapabilityProvider,
        aiCoreInferenceRepository = lazy { aiCoreInferenceRepository },
        liteRtInferenceRepository = lazy { liteRtInferenceRepository },
        aiModelResolver = aiModelResolver,
        localAiModelPackService = localAiModelPackService,
        defaultDispatcher = testDispatcher,
        liteRtInferenceTimeoutMs = liteRtInferenceTimeoutMs
    )
}
