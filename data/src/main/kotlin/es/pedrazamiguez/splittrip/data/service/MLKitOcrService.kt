package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Interface representing PDF first-page rendering logic, separated for unit-testability.
 */
internal interface PdfPageRenderer {
    fun renderFirstPage(uri: Uri): Bitmap
}

/**
 * Concrete implementation of [PdfPageRenderer] using Android's platform [PdfRenderer]
 * with OOM-safe downsampling bounded to a maximum page dimension.
 */
internal class PdfPageRendererImpl(private val context: Context) : PdfPageRenderer {
    override fun renderFirstPage(uri: Uri): Bitmap {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Could not open file descriptor for $uri")
        return parcelFileDescriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) {
                    throw IOException("PDF has no pages")
                }
                renderer.openPage(0).use { pg ->
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
        var renderedBitmap: Bitmap? = null
        return try {
            val mimeType = attachment.mimeType.lowercase(java.util.Locale.ROOT)
            require(mimeType in SUPPORTED_MIME_TYPES) {
                "Unsupported MIME type: ${attachment.mimeType}"
            }

            val uri = Uri.parse(attachment.localUri)

            // Perform potentially blocking/heavy resource loading on the IO dispatcher
            val inputImage = withContext(ioDispatcher) {
                try {
                    if (mimeType == "application/pdf") {
                        val bitmap = pdfPageRenderer.renderFirstPage(uri)
                        renderedBitmap = bitmap
                        InputImage.fromBitmap(bitmap, 0)
                    } else {
                        InputImage.fromFilePath(context, uri)
                    }
                } catch (e: CancellationException) {
                    renderedBitmap?.recycle()
                    renderedBitmap = null
                    throw e
                } catch (e: Exception) {
                    renderedBitmap?.recycle()
                    renderedBitmap = null
                    throw IOException("Failed to load image from URI: ${attachment.localUri}", e)
                }
            }

            // Perform CPU-heavy ML Kit OCR process and mapping on the Default dispatcher
            val ocrResult = withContext(defaultDispatcher) {
                ocrEngine.process(inputImage)
            }
            val blocks = ocrResult.blocks.map { blockText ->
                TextBlock(
                    text = blockText,
                    confidence = null
                )
            }.toImmutableList()

            // Safe length/blocks count log to prevent exposing raw financial PII data
            Timber.d("OCR completed: extracted ${blocks.size} blocks (${ocrResult.text.length} chars)")

            Result.success(
                RawReceiptText(
                    fullText = ocrResult.text,
                    blocks = blocks,
                    recognisedAt = Instant.now()
                )
            )
        } catch (e: CancellationException) {
            renderedBitmap?.recycle()
            renderedBitmap = null
            throw e
        } catch (e: Exception) {
            renderedBitmap?.recycle()
            renderedBitmap = null
            Result.failure(e)
        } finally {
            renderedBitmap?.recycle()
        }
    }

    private companion object {
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
