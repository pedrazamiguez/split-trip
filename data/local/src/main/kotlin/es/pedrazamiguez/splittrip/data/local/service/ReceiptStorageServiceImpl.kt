package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Copies a user-selected file from a content:// or file:// URI into the app's
 * private files directory (`filesDir/receipts/`) and compresses image types to
 * WebP lossless to reduce on-device storage.
 *
 * PDFs and other non-image MIME types are copied verbatim with their original extension.
 *
 * The resulting file is stable across app restarts — it does not depend on the
 * transient content:// URI granted by the OS picker.
 */
internal class ReceiptStorageServiceImpl(
    private val context: Context
) : ReceiptStorageService {

    override suspend fun copyAndCompress(sourceUri: String): ReceiptAttachment =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: FALLBACK_MIME

            val receiptsDir = File(context.filesDir, RECEIPTS_DIR).also { it.mkdirs() }
            val uniqueId = UUID.randomUUID().toString()

            val (destFile, actualMime) = if (mimeType.startsWith("image/")) {
                compressImage(resolver, uri, receiptsDir, uniqueId)
            } else {
                copyVerbatim(resolver, uri, receiptsDir, uniqueId, mimeType)
            }

            Timber.d("Receipt saved: ${destFile.absolutePath} ($actualMime)")

            ReceiptAttachment(
                localUri = destFile.absolutePath,
                mimeType = actualMime,
                capturedAtMillis = System.currentTimeMillis()
            )
        }

    /**
     * Decodes the source bitmap and re-encodes it as WebP lossless.
     * WebP delivers significantly smaller files than JPEG/PNG without quality loss,
     * reducing both on-device storage and Firebase Storage bandwidth.
     * Returns the saved file and its MIME type.
     */
    private fun compressImage(
        resolver: ContentResolver,
        uri: Uri,
        dir: File,
        uniqueId: String
    ): Pair<File, String> {
        val destFile = File(dir, "$uniqueId$WEBP_EXT")
        resolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
                ?: throw IllegalStateException("Could not decode image from $uri")
            FileOutputStream(destFile).use { output ->
                @Suppress("DEPRECATION") // WEBP_LOSSLESS added in API 30; WEBP is lossless on pre-30
                val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                bitmap.compress(format, WEBP_QUALITY, output)
                bitmap.recycle()
            }
        } ?: throw IllegalStateException("Could not open input stream for $uri")
        return destFile to WEBP_MIME
    }

    /** Streams the source content directly to a file without decoding, preserving the original bytes. */
    private fun copyVerbatim(
        resolver: ContentResolver,
        uri: Uri,
        dir: File,
        uniqueId: String,
        mimeType: String
    ): Pair<File, String> {
        val ext = MIME_TO_EXT[mimeType] ?: DEFAULT_EXT
        val destFile = File(dir, "$uniqueId$ext")
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Could not open input stream for $uri")
        return destFile to mimeType
    }

    private companion object {
        const val RECEIPTS_DIR = "receipts"
        const val WEBP_EXT = ".webp"
        const val WEBP_MIME = "image/webp"
        const val FALLBACK_MIME = "application/octet-stream"
        const val DEFAULT_EXT = ".bin"

        // Quality param is ignored for WEBP_LOSSLESS but required by the API signature.
        const val WEBP_QUALITY = 100
        val MIME_TO_EXT = mapOf(
            "application/pdf" to ".pdf",
            "image/jpeg" to ".jpg",
            "image/png" to ".png",
            "image/gif" to ".gif"
        )
    }
}
