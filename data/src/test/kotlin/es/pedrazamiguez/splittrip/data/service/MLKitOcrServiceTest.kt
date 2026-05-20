package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import io.mockk.clearAllMocks
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
    private lateinit var recogniser: TextRecognizer
    private lateinit var pdfPageRenderer: PdfPageRenderer
    private lateinit var service: MLKitOcrService

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        recogniser = mockk(relaxed = true)
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
            recogniser = recogniser
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

        // Construct real Text and Text.TextBlock instances using Java reflection to bypass MockK class transformation limits.
        val block1 = createTextBlock("Line 1")
        val block2 = createTextBlock("Line 2")
        val textInstance = createText("Full OCR text", listOf(block1, block2))

        val completedTask = Tasks.forResult(textInstance)
        every { recogniser.process(mockInputImage) } returns completedTask

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

        // Mock Bitmap
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        every { pdfPageRenderer.renderFirstPage(any()) } returns mockBitmap

        val mockInputImage = mockk<InputImage>()
        every { InputImage.fromBitmap(mockBitmap, 0) } returns mockInputImage

        val textInstance = createText("PDF Page 1 text", emptyList())

        val completedTask = Tasks.forResult(textInstance)
        every { recogniser.process(mockInputImage) } returns completedTask

        val result = service.recogniseText(attachment)

        assertTrue(result.isSuccess)
        assertEquals("PDF Page 1 text", result.getOrThrow().fullText)
        verify { mockBitmap.recycle() } // Verify bitmap was recycled
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

        val failedTask = Tasks.forException<Text>(Exception("OCR Engine Error"))
        every { recogniser.process(mockInputImage) } returns failedTask

        val result = service.recogniseText(attachment)

        assertTrue(result.isFailure)
        assertEquals("OCR Engine Error", result.exceptionOrNull()?.message)
    }

    private fun createText(text: String, textBlocks: List<Text.TextBlock>): Text {
        val constructor = Text::class.java.declaredConstructors.first {
            it.parameterTypes.size == 2 && it.parameterTypes[0] == String::class.java
        }
        constructor.isAccessible = true
        return constructor.newInstance(text, textBlocks) as Text
    }

    private fun createTextBlock(text: String): Text.TextBlock {
        val constructor = Text.TextBlock::class.java.declaredConstructors.first {
            it.parameterTypes.size == 6 && it.parameterTypes[0] == String::class.java
        }
        constructor.isAccessible = true
        return constructor.newInstance(
            text, // text
            null, // boundingBox
            emptyList<Any>(), // cornerPoints
            "en", // recognizedLanguage
            null, // frameToCanvasMatrix
            emptyList<Any>() // lines
        ) as Text.TextBlock
    }
}
