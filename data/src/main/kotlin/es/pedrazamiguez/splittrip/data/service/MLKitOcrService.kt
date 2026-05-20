package es.pedrazamiguez.splittrip.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import java.io.IOException
import java.time.Instant
import kotlinx.collections.immutable.toImmutableList
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
 * Concrete implementation of [PdfPageRenderer] using Android's platform [PdfRenderer].
 */
internal class PdfPageRendererImpl(private val context: Context) : PdfPageRenderer {
    override fun renderFirstPage(uri: Uri): Bitmap {
        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Could not open file descriptor for $uri")
        parcelFileDescriptor.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            pdfRenderer.use { renderer ->
                if (renderer.pageCount == 0) {
                    throw IOException("PDF has no pages")
                }
                val page = renderer.openPage(0)
                page.use { pg ->
                    val bitmap = Bitmap.createBitmap(pg.width, pg.height, Bitmap.Config.ARGB_8888)
                    pg.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return bitmap
                }
            }
        }
    }
}

/**
 * Concrete implementation of [ReceiptOcrService] backed by Google ML Kit's
 * Text Recognition v2 (Latin script). Runs fully offline.
 */
internal class MLKitOcrService(
    private val context: Context,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val pdfPageRenderer: PdfPageRenderer = PdfPageRendererImpl(context)
) : ReceiptOcrService {

    private val recogniser = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recogniseText(attachment: ReceiptAttachment): Result<RawReceiptText> =
        withContext(defaultDispatcher) {
            runCatching {
                val mimeType = attachment.mimeType.lowercase()
                if (mimeType !in SUPPORTED_MIME_TYPES) {
                    throw IllegalArgumentException("Unsupported MIME type: ${attachment.mimeType}")
                }

                val uri = Uri.parse(attachment.localUri)
                var renderedBitmap: Bitmap? = null
                val inputImage = try {
                    if (mimeType == "application/pdf") {
                        val bitmap = pdfPageRenderer.renderFirstPage(uri)
                        renderedBitmap = bitmap
                        InputImage.fromBitmap(bitmap, 0)
                    } else {
                        InputImage.fromFilePath(context, uri)
                    }
                } catch (e: Exception) {
                    throw IOException("Failed to load image from URI: ${attachment.localUri}", e)
                }

                try {
                    val visionText = recogniser.process(inputImage).await()
                    Timber.d("OCR raw text: ${visionText.text}")

                    val blocks = visionText.textBlocks.map { block ->
                        TextBlock(
                            text = block.text,
                            confidence = null
                        )
                    }.toImmutableList()

                    RawReceiptText(
                        fullText = visionText.text,
                        blocks = blocks,
                        recognisedAt = Instant.now()
                    )
                } finally {
                    renderedBitmap?.recycle()
                }
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
