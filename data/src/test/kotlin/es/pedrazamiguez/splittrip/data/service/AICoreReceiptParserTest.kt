package es.pedrazamiguez.splittrip.data.service

import com.google.ai.edge.aicore.Candidate
import com.google.ai.edge.aicore.GenerateContentResponse
import com.google.ai.edge.aicore.GenerativeModel
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AICoreReceiptParserTest {

    private lateinit var generativeModel: GenerativeModel
    private lateinit var parser: AICoreReceiptParser

    @BeforeEach
    fun setUp() {
        val context = mockk<android.content.Context>(relaxed = true)
        val resources = mockk<android.content.res.Resources>(relaxed = true)
        every { context.resources } returns resources
        every { resources.openRawResource(any()) } throws android.content.res.Resources.NotFoundException()

        generativeModel = mockk(relaxed = true)
        parser = AICoreReceiptParser(context, generativeModel)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    // ── smartTruncate — no truncation needed ──────────────────────────────────

    @Test
    fun `smartTruncate returns text unchanged when below budget`() {
        val text = "A".repeat(2_999)
        assertEquals(text, AICoreReceiptParser.smartTruncate(text))
    }

    @Test
    fun `smartTruncate returns text unchanged when exactly at budget`() {
        val text = "A".repeat(3_000)
        assertEquals(text, AICoreReceiptParser.smartTruncate(text))
    }

    // ── smartTruncate — truncation path ───────────────────────────────────────

    @Test
    fun `smartTruncate result length never exceeds budget when input is long`() {
        val text = "X".repeat(6_000)
        val result = AICoreReceiptParser.smartTruncate(text)
        assertTrue(result.length <= 3_000, "Expected ≤3000 chars, got ${result.length}")
    }

    @Test
    fun `smartTruncate preserves merchant name at the head`() {
        val airlineName = "AIRLINE NAME BOOKING REF ".repeat(24) // 600 chars exactly
        val fareDetails = "FARE BREAKDOWN GRAND TOTAL ".repeat(200) // long tail region
        val text = airlineName + fareDetails

        val result = AICoreReceiptParser.smartTruncate(text)

        assertTrue(
            result.startsWith(airlineName),
            "Head of result should start with the merchant/airline name block"
        )
    }

    @Test
    fun `smartTruncate preserves grand total at the tail`() {
        val header = "HEADER DATA ".repeat(300) // long, will be truncated
        val grandTotalLine = "GRAND TOTAL 1234.56 EUR 2026-05-22"
        val fareSection = "item ".repeat(400) + grandTotalLine

        val text = header + fareSection

        val result = AICoreReceiptParser.smartTruncate(text)

        assertTrue(
            result.endsWith(grandTotalLine),
            "Tail of result should contain the grand total line"
        )
    }

    @Test
    fun `smartTruncate inserts ellipsis separator between head and tail`() {
        val text = "A".repeat(6_000)
        val result = AICoreReceiptParser.smartTruncate(text)
        assertTrue(result.contains("\n…\n"), "Truncated result must contain the ellipsis separator")
    }

    // ── parse ─────────────────────────────────────────────────────────────────

    @Test
    fun `parse returns empty receipt when raw text is blank`() = runTest {
        val rawText = RawReceiptText(
            fullText = "   ",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertNull(receipt.amount)
        assertNull(receipt.currency)
        assertNull(receipt.date)
        assertNull(receipt.title)
        assertEquals(ExtractionSource.AI_CORE, receipt.source)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
    }

    @Test
    fun `parse parses valid JSON with all fields as HIGH confidence`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34 EUR 2026-05-20",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "amount": "12.34",
                "currency": "EUR",
                "date": "2026-05-20",
                "title": "Store"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("12.34"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 20), receipt.date)
        assertEquals("Store", receipt.title)
        assertEquals(ExtractionConfidence.HIGH, receipt.confidence)

        coVerify(exactly = 1) { generativeModel.prepareInferenceEngine() }
    }

    @Test
    fun `parse parses valid JSON with three fields as MEDIUM confidence`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34 2026-05-20",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "amount": "12.34",
                "date": "2026-05-20",
                "title": "Store"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("12.34"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertEquals(LocalDate.of(2026, 5, 20), receipt.date)
        assertEquals("Store", receipt.title)
        assertEquals(ExtractionConfidence.MEDIUM, receipt.confidence)
    }

    @Test
    fun `parse parses valid JSON with two fields as MEDIUM confidence`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "amount": "12.34",
                "title": "Store"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("12.34"), receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertNull(receipt.date)
        assertEquals("Store", receipt.title)
        assertEquals(ExtractionConfidence.MEDIUM, receipt.confidence)
    }

    @Test
    fun `parse parses valid JSON with one field as LOW confidence`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "title": "Store"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertNull(receipt.amount)
        assertEquals("EUR", receipt.currency)
        assertNull(receipt.date)
        assertEquals("Store", receipt.title)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
    }

    @Test
    fun `parse returns empty receipt when response is not valid JSON`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val nonJsonResponse = "This is not a JSON object"

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns nonJsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertNull(receipt.amount)
        assertNull(receipt.currency)
        assertNull(receipt.date)
        assertNull(receipt.title)
        assertEquals(ExtractionConfidence.LOW, receipt.confidence)
    }

    @Test
    fun `parse propagates exception when inference fails`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val exception = RuntimeException("Inference engine error")

        coEvery { generativeModel.generateContent(any<String>()) } throws exception

        val result = parser.parse(rawText)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `parse rethrows CancellationException`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Store 12.34",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val cancellationException = CancellationException("Job cancelled")

        coEvery { generativeModel.generateContent(any<String>()) } throws cancellationException

        try {
            parser.parse(rawText)
            org.junit.jupiter.api.Assertions.fail("Expected CancellationException to be thrown")
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }

    @Test
    fun `parse loads prompt from resources when available`() = runTest {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        val mockResources = mockk<android.content.res.Resources>(relaxed = true)
        every { mockContext.resources } returns mockResources
        val promptStream = java.io.ByteArrayInputStream("Custom prompt %1\$s".toByteArray())
        every { mockResources.openRawResource(any()) } returns promptStream

        val localParser = AICoreReceiptParser(mockContext, generativeModel)

        val rawText = RawReceiptText(
            fullText = "Store 12.34",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns "{}"
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = localParser.parse(rawText)
        assertTrue(result.isSuccess)

        coVerify { generativeModel.generateContent("Custom prompt Store 12.34") }
    }

    @Test
    fun `parse cleans and normalizes amount with commas and currency symbols`() = runTest {
        val rawText = RawReceiptText(
            fullText = "24,20€",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "amount": "24,20€",
                "currency": "EUR"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals(BigDecimal("24.20"), receipt.amount)
        assertEquals("EUR", receipt.currency)
    }

    @Test
    fun `parse extracts notes field from JSON response`() = runTest {
        val rawText = RawReceiptText(
            fullText = "Locator: ABC123D",
            blocks = persistentListOf(),
            recognisedAt = Instant.now()
        )
        val jsonResponse = """
            {
                "notes": "Locator: ABC123D"
            }
        """.trimIndent()

        val mockResponse = mockk<GenerateContentResponse>()
        val mockCandidate = mockk<Candidate>()
        every { mockResponse.text } returns jsonResponse
        every { mockResponse.candidates } returns listOf(mockCandidate)
        every { mockCandidate.finishReason } returns Candidate.FinishReason.STOP

        coEvery { generativeModel.generateContent(any<String>()) } returns mockResponse

        val result = parser.parse(rawText)

        assertTrue(result.isSuccess)
        val receipt = result.getOrThrow()
        assertEquals("Locator: ABC123D", receipt.notes)
    }
}
