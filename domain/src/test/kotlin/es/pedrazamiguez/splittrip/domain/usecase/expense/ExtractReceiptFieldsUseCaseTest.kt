package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExtractReceiptFieldsUseCaseTest {

    private lateinit var ocrService: ReceiptOcrService
    private lateinit var extractionService: ReceiptExtractionService
    private lateinit var useCase: ExtractReceiptFieldsUseCase

    private val attachment = ReceiptAttachment(
        localUri = "file:///path/to/receipt.webp",
        mimeType = "image/webp",
        capturedAtMillis = 1000L
    )

    private val rawText = RawReceiptText(
        fullText = "Dinner Total: 50.00 EUR",
        blocks = persistentListOf(),
        recognisedAt = Instant.now()
    )

    private val extractedReceipt = ExtractedReceipt(
        amount = BigDecimal("50.00"),
        currency = "EUR",
        date = LocalDate.of(2026, 5, 23),
        time = java.time.LocalTime.of(19, 30),
        title = "Dinner",
        vendor = "Restaurant",
        category = null,
        paymentMethod = "CASH",
        notes = null,
        source = ExtractionSource.AI_CORE,
        confidence = ExtractionConfidence.HIGH
    )

    @BeforeEach
    fun setUp() {
        ocrService = mockk()
        extractionService = mockk()
        useCase = ExtractReceiptFieldsUseCase(ocrService, extractionService)
    }

    @Test
    fun `invoke processes OCR and extraction successfully`() = runTest {
        coEvery { ocrService.recogniseText(attachment) } returns Result.success(rawText)
        coEvery { extractionService.extract(rawText) } returns Result.success(extractedReceipt)

        val result = useCase(attachment)

        assertTrue(result.isSuccess)
        assertEquals(extractedReceipt, result.getOrNull())

        coVerify(exactly = 1) { ocrService.recogniseText(attachment) }
        coVerify(exactly = 1) { extractionService.extract(rawText) }
    }

    @Test
    fun `invoke returns failure when OCR fails`() = runTest {
        val ocrException = RuntimeException("OCR failed")
        coEvery { ocrService.recogniseText(attachment) } returns Result.failure(ocrException)

        val result = useCase(attachment)

        assertTrue(result.isFailure)
        assertEquals(ocrException, result.exceptionOrNull())

        coVerify(exactly = 1) { ocrService.recogniseText(attachment) }
        coVerify(exactly = 0) { extractionService.extract(any()) }
    }

    @Test
    fun `invoke returns failure when extraction fails`() = runTest {
        val extractionException = RuntimeException("Extraction failed")
        coEvery { ocrService.recogniseText(attachment) } returns Result.success(rawText)
        coEvery { extractionService.extract(rawText) } returns Result.failure(extractionException)

        val result = useCase(attachment)

        assertTrue(result.isFailure)
        assertEquals(extractionException, result.exceptionOrNull())

        coVerify(exactly = 1) { ocrService.recogniseText(attachment) }
        coVerify(exactly = 1) { extractionService.extract(rawText) }
    }
}
