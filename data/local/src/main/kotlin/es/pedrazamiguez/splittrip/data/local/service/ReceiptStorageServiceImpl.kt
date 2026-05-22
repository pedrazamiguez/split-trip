package es.pedrazamiguez.splittrip.data.local.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
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
 * WebP to reduce on-device storage.
 *
 * On API 30+, `WEBP_LOSSLESS` guarantees lossless compression.
 * On API < 30, the legacy `WEBP` format is used; this is lossy at quality < 100
 * but since [WEBP_QUALITY] is set to 100 the output is visually lossless in practice.
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
            val finalMime = resolveMimeType(uri, sourceUri)

            val receiptsDir = File(context.filesDir, RECEIPTS_DIR).also { it.mkdirs() }
            val uniqueId = UUID.randomUUID().toString()

            val tempFile = File(context.cacheDir, "temp_receipt_$uniqueId").also { it.parentFile?.mkdirs() }
            try {
                copyToTempFile(uri, tempFile)

                val (destFile, actualMime) = if (finalMime.startsWith("image/")) {
                    compressImage(tempFile, receiptsDir, uniqueId)
                } else {
                    copyVerbatim(tempFile, receiptsDir, uniqueId, finalMime)
                }

                Timber.d("Receipt saved: ${destFile.absolutePath} ($actualMime)")

                ReceiptAttachment(
                    localUri = destFile.toUri().toString(),
                    mimeType = actualMime,
                    capturedAtMillis = System.currentTimeMillis()
                )
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                cleanUpSourceCameraFile(uri, sourceUri)
            }
        }

    override suspend fun downloadAndStore(remoteUrl: String): ReceiptAttachment =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(remoteUrl)
            val receiptsDir = File(context.filesDir, RECEIPTS_DIR).also { it.mkdirs() }
            val uniqueId = UUID.randomUUID().toString()
            val tempFile = File(context.cacheDir, "download_receipt_$uniqueId").also { it.parentFile?.mkdirs() }

            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL(remoteUrl)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = CONNECTION_TIMEOUT_MS
                connection.readTimeout = CONNECTION_TIMEOUT_MS
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    error("Failed to download file: HTTP ${connection.responseCode}")
                }

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var contentType = connection.contentType ?: ""
                if (contentType.contains(";")) {
                    contentType = contentType.substringBefore(";")
                }

                val finalMime = if (contentType.isNotBlank() && contentType != "application/octet-stream") {
                    contentType
                } else {
                    resolveMimeType(uri, remoteUrl)
                }

                val (destFile, actualMime) = if (finalMime.startsWith("image/")) {
                    compressImage(tempFile, receiptsDir, uniqueId)
                } else {
                    copyVerbatim(tempFile, receiptsDir, uniqueId, finalMime)
                }

                ReceiptAttachment(
                    localUri = destFile.toUri().toString(),
                    mimeType = actualMime,
                    capturedAtMillis = System.currentTimeMillis(),
                    remoteUrl = remoteUrl
                )
            } finally {
                connection?.disconnect()
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

    private fun resolveMimeType(uri: Uri, sourceUri: String): String {
        val resolver = context.contentResolver
        var mimeType = resolver.getType(uri)
        if (mimeType.isNullOrBlank() || mimeType == FALLBACK_MIME) {
            val fileExtension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(sourceUri)
                .ifBlank {
                    val path = uri.path.orEmpty()
                    val dotIndex = path.lastIndexOf('.')
                    if (dotIndex != -1) path.substring(dotIndex + 1) else ""
                }
            if (fileExtension.isNotEmpty()) {
                val inferredMime = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(fileExtension.lowercase())
                if (!inferredMime.isNullOrBlank()) {
                    mimeType = inferredMime
                }
            }
        }
        return mimeType ?: FALLBACK_MIME
    }

    private fun copyToTempFile(uri: Uri, tempFile: File) {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Could not open input stream for $uri")
    }

    private fun cleanUpSourceCameraFile(uri: Uri, sourceUri: String) {
        if (uri.authority == "${context.packageName}.fileprovider" &&
            uri.lastPathSegment?.startsWith("camera_") == true
        ) {
            try {
                val sourceFile = File(File(context.filesDir, RECEIPTS_DIR), uri.lastPathSegment!!)
                if (sourceFile.exists()) {
                    sourceFile.delete()
                    Timber.d("Camera temp file deleted: ${sourceFile.absolutePath}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete camera temp file: $sourceUri")
            }
        }
    }

    /**
     * Decodes the source bitmap with OOM-safe downsampling (max [MAX_IMAGE_DIMENSION] px on
     * either axis) and re-encodes it as WebP.
     *
     * On API 30+ `WEBP_LOSSLESS` is used (truly lossless).
     * On API < 30 the legacy `WEBP` encoder is used at quality 100; it is not guaranteed
     * lossless by the spec but in practice produces visually lossless output.
     *
     * Returns the saved file and its MIME type.
     */
    private fun compressImage(
        tempFile: File,
        dir: File,
        uniqueId: String
    ): Pair<File, String> {
        // Pass 1: decode only bounds from temporary file — no pixels allocated
        val boundsOpts = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tempFile.absolutePath, boundsOpts)

        // Compute a power-of-2 sample size so neither dimension exceeds MAX_IMAGE_DIMENSION
        val inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight)

        // Pass 2: decode at reduced resolution to avoid OOM on high-res photos
        val bitmap = BitmapFactory.decodeFile(
            tempFile.absolutePath,
            BitmapFactory.Options().also { it.inSampleSize = inSampleSize }
        ) ?: error("Could not decode image from temporary file")

        val destFile = File(dir, "$uniqueId$WEBP_EXT")
        FileOutputStream(destFile).use { output ->
            @Suppress("DEPRECATION") // WEBP_LOSSLESS added in API 30; WEBP used on < 30 (see KDoc)
            val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, WEBP_QUALITY, output)
            bitmap.recycle()
        }
        return destFile to WEBP_MIME
    }

    /** Streams the source content directly to a file without decoding, preserving the original bytes. */
    private fun copyVerbatim(
        tempFile: File,
        dir: File,
        uniqueId: String,
        mimeType: String
    ): Pair<File, String> {
        val ext = MIME_TO_EXT[mimeType] ?: DEFAULT_EXT
        val destFile = File(dir, "$uniqueId$ext")
        if (!tempFile.renameTo(destFile)) {
            // fallback if rename fails (e.g. across partitions)
            tempFile.copyTo(destFile, overwrite = true)
        }
        return destFile to mimeType
    }

    /**
     * Returns the smallest power-of-2 sample size that keeps both dimensions within
     * [MAX_IMAGE_DIMENSION]. Returns 1 when the image is already small enough.
     */
    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var inSampleSize = 1
        while ((height / inSampleSize) > MAX_IMAGE_DIMENSION || (width / inSampleSize) > MAX_IMAGE_DIMENSION) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private companion object {
        const val RECEIPTS_DIR = "receipts"
        const val CONNECTION_TIMEOUT_MS = 10000
        const val WEBP_EXT = ".webp"
        const val WEBP_MIME = "image/webp"
        const val FALLBACK_MIME = "application/octet-stream"
        const val DEFAULT_EXT = ".bin"

        // Quality param is ignored for WEBP_LOSSLESS but required by the API signature.
        const val WEBP_QUALITY = 100

        /** Max width/height in pixels before downsampling kicks in. */
        const val MAX_IMAGE_DIMENSION = 2048

        val MIME_TO_EXT = mapOf(
            "application/pdf" to ".pdf",
            "image/jpeg" to ".jpg",
            "image/png" to ".png",
            "image/gif" to ".gif"
        )
    }
}
