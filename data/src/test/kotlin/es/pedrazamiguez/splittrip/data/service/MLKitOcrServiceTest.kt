package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MLKitOcrServiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var ocrEngine: OcrEngine
    private lateinit var pdfPageRenderer: PdfPageRenderer
    private lateinit var service: MLKitOcrService

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        ocrEngine = mockk(relaxed = true)
        pdfPageRenderer = mockk(relaxed = true)

        mockkStatic(InputImage::class)
        mockkStatic(Uri::class)

        // Mock Uri.parse to return a mocked Uri
        val mockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns mockUri

        service = MLKitOcrService(
            context = context,
            defaultDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
            pdfPageRenderer = pdfPageRenderer,
            ocrEngine = ocrEngine
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `recogniseText with image returns success and maps text blocks`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.jpg",
            mimeType = "image/jpeg",
            capturedAtMillis = 123456789L
        )

        val mockInputImage = mockk<InputImage>()
        every { InputImage.fromFilePath(context, any<Uri>()) } returns mockInputImage

        val ocrResult = OcrResult("Full OCR text", listOf("Line 1", "Line 2"))
        coEvery { ocrEngine.process(mockInputImage) } returns ocrResult

        val result = service.recogniseText(attachment)

        assertTrue(result.isSuccess)
        val rawText = result.getOrThrow()
        assertEquals("Full OCR text", rawText.fullText)
        assertEquals(2, rawText.blocks.size)
        assertEquals("Line 1", rawText.blocks[0].text)
        assertNull(rawText.blocks[0].confidence)
        assertEquals("Line 2", rawText.blocks[1].text)
        assertNotNull(rawText.recognisedAt)
    }

    @Test
    fun `recogniseText with PDF renders first page and runs OCR`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 1

        // Mock Bitmap
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { pdfPageRenderer.renderPage(any(), 0) } returns mockBitmap

        val mockInputImage = mockk<InputImage>()
        every { InputImage.fromBitmap(mockBitmap, 0) } returns mockInputImage

        val ocrResult = OcrResult("PDF Page 1 text", emptyList())
        coEvery { ocrEngine.process(mockInputImage) } returns ocrResult

        val result = service.recogniseText(attachment)

        assertTrue(result.isSuccess)
        assertEquals("PDF Page 1 text", result.getOrThrow().fullText)
        verify { mockBitmap.recycle() } // Verify bitmap was recycled
    }

    @Test
    fun `recogniseText with multi-page PDF renders pages and accumulates OCR results`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 3

        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        val mockBitmap2 = mockk<Bitmap>(relaxed = true)
        val mockBitmap3 = mockk<Bitmap>(relaxed = true)
        every { pdfPageRenderer.renderPage(any(), 0) } returns mockBitmap1
        every { pdfPageRenderer.renderPage(any(), 1) } returns mockBitmap2
        every { pdfPageRenderer.renderPage(any(), 2) } returns mockBitmap3

        val mockImage1 = mockk<InputImage>()
        val mockImage2 = mockk<InputImage>()
        val mockImage3 = mockk<InputImage>()
        every { InputImage.fromBitmap(mockBitmap1, 0) } returns mockImage1
        every { InputImage.fromBitmap(mockBitmap2, 0) } returns mockImage2
        every { InputImage.fromBitmap(mockBitmap3, 0) } returns mockImage3

        coEvery { ocrEngine.process(mockImage1) } returns OcrResult("Page 1 Text", listOf("Block 1.1", "Block 1.2"))
        coEvery { ocrEngine.process(mockImage2) } returns OcrResult("Page 2 Text", listOf("Block 2.1"))
        coEvery { ocrEngine.process(mockImage3) } returns OcrResult("Page 3 Text", listOf("Block 3.1"))

        val result = service.recogniseText(attachment)

        assertTrue(result.isSuccess)
        val rawText = result.getOrThrow()
        assertEquals("Page 1 Text\n\nPage 2 Text\n\nPage 3 Text", rawText.fullText)
        assertEquals(4, rawText.blocks.size)
        assertEquals("Block 1.1", rawText.blocks[0].text)
        assertEquals("Block 1.2", rawText.blocks[1].text)
        assertEquals("Block 2.1", rawText.blocks[2].text)
        assertEquals("Block 3.1", rawText.blocks[3].text)

        verify { mockBitmap1.recycle() }
        verify { mockBitmap2.recycle() }
        verify { mockBitmap3.recycle() }
    }

    @Test
    fun `recogniseText with PDF exceeding max pages limits processing to 5 pages`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 7

        val mockBitmaps = List(7) { mockk<Bitmap>(relaxed = true) }
        val mockImages = List(7) { mockk<InputImage>() }

        for (i in 0 until 7) {
            every { pdfPageRenderer.renderPage(any(), i) } returns mockBitmaps[i]
            every { InputImage.fromBitmap(mockBitmaps[i], 0) } returns mockImages[i]
            coEvery { ocrEngine.process(mockImages[i]) } returns OcrResult("Text $i", emptyList())
        }

        val result = service.recogniseText(attachment)

        assertTrue(result.isSuccess)
        val rawText = result.getOrThrow()
        assertEquals("Text 0\n\nText 1\n\nText 2\n\nText 3\n\nText 4", rawText.fullText)

        verify(exactly = 5) { pdfPageRenderer.renderPage(any(), any()) }
        verify(exactly = 0) { pdfPageRenderer.renderPage(any(), 5) }
        verify(exactly = 0) { pdfPageRenderer.renderPage(any(), 6) }

        verify(exactly = 1) { mockBitmaps[0].recycle() }
        verify(exactly = 1) { mockBitmaps[1].recycle() }
        verify(exactly = 1) { mockBitmaps[2].recycle() }
        verify(exactly = 1) { mockBitmaps[3].recycle() }
        verify(exactly = 1) { mockBitmaps[4].recycle() }
        verify(exactly = 0) { mockBitmaps[5].recycle() }
        verify(exactly = 0) { mockBitmaps[6].recycle() }
    }

    @Test
    fun `recogniseText when PDF page rendering throws exception continues to clean up and returns failure`() = runTest(
        testDispatcher
    ) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 3

        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        every { pdfPageRenderer.renderPage(any(), 0) } returns mockBitmap1
        every { pdfPageRenderer.renderPage(any(), 1) } throws IOException("Rendering error")

        val mockImage1 = mockk<InputImage>()
        every { InputImage.fromBitmap(mockBitmap1, 0) } returns mockImage1
        coEvery { ocrEngine.process(mockImage1) } returns OcrResult("Page 1 Text", emptyList())

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IOException)
        assertTrue(exception?.message?.contains("Failed to load or process PDF page 1") == true)

        verify { mockBitmap1.recycle() }
    }

    @Test
    fun `recogniseText when PDF page count reading throws exception returns failure`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } throws IOException("Page count error")

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IOException)
        assertTrue(exception?.message?.contains("Failed to read PDF page count") == true)
    }

    @Test
    fun `recogniseText when PDF page count reading throws CancellationException rethrows`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } throws
            kotlinx.coroutines.CancellationException("Page count cancelled")

        try {
            service.recogniseText(attachment)
            org.junit.jupiter.api.Assertions.fail("Expected CancellationException")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertEquals("Page count cancelled", e.message)
        }
    }

    @Test
    fun `recogniseText when PDF has 0 pages returns failure`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 0

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IOException)
        assertEquals("PDF has no pages", exception?.message)
    }

    @Test
    fun `recogniseText when PDF cancellation occurs recycles bitmap and rethrows`() = runTest(
        testDispatcher
    ) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.pdf",
            mimeType = "application/pdf",
            capturedAtMillis = 123456789L
        )

        every { pdfPageRenderer.getPageCount(any()) } returns 3

        val mockBitmap1 = mockk<Bitmap>(relaxed = true)
        val mockBitmap2 = mockk<Bitmap>(relaxed = true)
        every { pdfPageRenderer.renderPage(any(), 0) } returns mockBitmap1
        every { pdfPageRenderer.renderPage(any(), 1) } returns mockBitmap2

        val mockImage1 = mockk<InputImage>()
        val mockImage2 = mockk<InputImage>()
        every { InputImage.fromBitmap(mockBitmap1, 0) } returns mockImage1
        every { InputImage.fromBitmap(mockBitmap2, 0) } returns mockImage2

        coEvery { ocrEngine.process(mockImage1) } returns OcrResult("Page 1 Text", emptyList())
        coEvery { ocrEngine.process(mockImage2) } throws kotlinx.coroutines.CancellationException("Cancelled")

        try {
            service.recogniseText(attachment)
            org.junit.jupiter.api.Assertions.fail("Expected CancellationException")
        } catch (e: kotlinx.coroutines.CancellationException) {
            assertEquals("Cancelled", e.message)
        }

        verify { mockBitmap1.recycle() }
        verify { mockBitmap2.recycle() }
    }

    @Test
    fun `recogniseText with unsupported mime type returns failure`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.zip",
            mimeType = "application/zip",
            capturedAtMillis = 123456789L
        )

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Unsupported MIME type: application/zip", result.exceptionOrNull()?.message)
    }

    @Test
    fun `recogniseText when image loading throws IOException returns failure`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/missing.jpg",
            mimeType = "image/jpeg",
            capturedAtMillis = 123456789L
        )

        every { InputImage.fromFilePath(context, any<Uri>()) } throws IOException("File not found")

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to load image from URI") == true)
    }

    @Test
    fun `recogniseText when OCR engine fails returns failure`() = runTest(testDispatcher) {
        val attachment = ReceiptAttachment(
            localUri = "file:///path/to/receipt.jpg",
            mimeType = "image/jpeg",
            capturedAtMillis = 123456789L
        )

        val mockInputImage = mockk<InputImage>()
        every { InputImage.fromFilePath(context, any<Uri>()) } returns mockInputImage

        coEvery { ocrEngine.process(mockInputImage) } throws Exception("OCR Engine Error")

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        assertEquals("OCR Engine Error", result.exceptionOrNull()?.message)
    }
}
