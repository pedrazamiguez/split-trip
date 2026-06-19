package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import java.io.IOException
import java.time.Instant
import java.util.Locale
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Interface representing PDF rendering logic (page count and page-specific rendering), separated for unit-testability.
 */
internal interface PdfPageRenderer {
    fun renderFirstPage(uri: Uri): Bitmap
    fun getPageCount(uri: Uri): Int
    fun renderPage(uri: Uri, pageIndex: Int): Bitmap
}

/**
 * Concrete implementation of [PdfPageRenderer] using Android's platform [PdfRenderer]
 * with OOM-safe downsampling bounded to a maximum page dimension.
 */
internal class PdfPageRendererImpl(private val context: Context) : PdfPageRenderer {
    override fun renderFirstPage(uri: Uri): Bitmap = renderPage(uri, 0)

    override fun getPageCount(uri: Uri): Int {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Could not open file descriptor for $uri")
        return parcelFileDescriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.pageCount
            }
        }
    }

    override fun renderPage(uri: Uri, pageIndex: Int): Bitmap {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Could not open file descriptor for $uri")
        return parcelFileDescriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) {
                    throw IOException("PDF has no pages")
                }
                require(pageIndex in 0 until renderer.pageCount) {
                    "Page index $pageIndex out of bounds for PDF with ${renderer.pageCount} pages"
                }
                renderer.openPage(pageIndex).use { pg ->
                    renderPage(pg)
                }
            }
        }
    }

    private fun renderPage(page: PdfRenderer.Page): Bitmap {
        val maxDimension = 2048
        var width = page.width
        var height = page.height
        val maxDim = maxOf(width, height)
        if (maxDim > maxDimension) {
            val scale = maxDimension.toDouble() / maxDim
            width = (width * scale).toInt().coerceAtLeast(1)
            height = (height * scale).toInt().coerceAtLeast(1)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }
}

/**
 * Internal interface representing the OCR processor engine, abstracted for unit testability.
 */
internal interface OcrEngine {
    suspend fun process(image: InputImage): OcrResult
}

/**
 * DTO representing the output of the OCR processing engine.
 */
internal data class OcrResult(
    val text: String,
    val blocks: List<String>
)

/**
 * Concrete implementation of [OcrEngine] using Google's ML Kit [TextRecognizer].
 */
internal class MLKitOcrEngine(
    private val recogniser: TextRecognizer
) : OcrEngine {
    override suspend fun process(image: InputImage): OcrResult {
        val visionText = recogniser.process(image).await()
        return OcrResult(
            text = visionText.text,
            blocks = visionText.textBlocks.map { it.text }
        )
    }
}

/**
 * Concrete implementation of [ReceiptOcrService] backed by Google ML Kit's
 * Text Recognition v2 (Latin script). Runs fully offline.
 */
internal class MLKitOcrService(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pdfPageRenderer: PdfPageRenderer = PdfPageRendererImpl(context),
    private val ocrEngine: OcrEngine = MLKitOcrEngine(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS))
) : ReceiptOcrService {

    override suspend fun recogniseText(attachment: ReceiptAttachment): Result<RawReceiptText> {
        return try {
            val mimeType = attachment.mimeType.lowercase(Locale.ROOT)
            require(mimeType in SUPPORTED_MIME_TYPES) {
                "Unsupported MIME type: ${attachment.mimeType}"
            }

            val uri = Uri.parse(attachment.localUri)
            val fullTextBuilder = StringBuilder()
            val textBlocks = mutableListOf<TextBlock>()

            if (mimeType == "application/pdf") {
                processPdf(uri, fullTextBuilder, textBlocks)
            } else {
                processImage(uri, fullTextBuilder, textBlocks)
            }

            val finalFullText = fullTextBuilder.toString()
            val finalBlocks = textBlocks.toImmutableList()

            // Safe length/blocks count log to prevent exposing raw financial PII data
            Timber.d("OCR completed: extracted ${finalBlocks.size} blocks (${finalFullText.length} chars)")

            Result.success(
                RawReceiptText(
                    fullText = finalFullText,
                    blocks = finalBlocks,
                    recognisedAt = Instant.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processPdf(
        uri: Uri,
        fullTextBuilder: StringBuilder,
        textBlocks: MutableList<TextBlock>
    ) {
        val pageCount = readPageCount(uri)
        if (pageCount <= 0) {
            throw IOException("PDF has no pages")
        }
        val pagesToProcess = minOf(pageCount, MAX_PDF_PAGES)
        for (pageIndex in 0 until pagesToProcess) {
            processPdfPage(uri, pageIndex, fullTextBuilder, textBlocks)
        }
    }

    private suspend fun readPageCount(uri: Uri): Int = withContext(ioDispatcher) {
        try {
            pdfPageRenderer.getPageCount(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to read PDF page count from URI: $uri", e)
        }
    }

    private suspend fun processPdfPage(
        uri: Uri,
        pageIndex: Int,
        fullTextBuilder: StringBuilder,
        textBlocks: MutableList<TextBlock>
    ) {
        var renderedBitmap: Bitmap? = null
        try {
            renderedBitmap = withContext(ioDispatcher) {
                pdfPageRenderer.renderPage(uri, pageIndex)
            }
            val inputImage = InputImage.fromBitmap(renderedBitmap, 0)
            val ocrResult = withContext(defaultDispatcher) {
                ocrEngine.process(inputImage)
            }
            if (ocrResult.text.isNotEmpty()) {
                if (fullTextBuilder.isNotEmpty()) {
                    fullTextBuilder.append("\n\n")
                }
                fullTextBuilder.append(ocrResult.text)
            }
            ocrResult.blocks.forEach { blockText ->
                textBlocks.add(
                    TextBlock(
                        text = blockText,
                        confidence = null
                    )
                )
            }
        } catch (e: CancellationException) {
            renderedBitmap?.recycle()
            renderedBitmap = null
            throw e
        } catch (e: Exception) {
            renderedBitmap?.recycle()
            renderedBitmap = null
            throw IOException("Failed to load or process PDF page $pageIndex from URI: $uri", e)
        } finally {
            renderedBitmap?.recycle()
        }
    }

    private suspend fun processImage(
        uri: Uri,
        fullTextBuilder: StringBuilder,
        textBlocks: MutableList<TextBlock>
    ) {
        val inputImage = withContext(ioDispatcher) {
            try {
                InputImage.fromFilePath(context, uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw IOException("Failed to load image from URI: $uri", e)
            }
        }
        val ocrResult = withContext(defaultDispatcher) {
            ocrEngine.process(inputImage)
        }
        fullTextBuilder.append(ocrResult.text)
        ocrResult.blocks.forEach { blockText ->
            textBlocks.add(
                TextBlock(
                    text = blockText,
                    confidence = null
                )
            )
        }
    }

    private companion object {
        const val MAX_PDF_PAGES = 5

        val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/bmp",
            "application/pdf"
        )
    }
}
