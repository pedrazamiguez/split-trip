package es.pedrazamiguez.splittrip.data.local.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import es.pedrazamiguez.splittrip.domain.model.CropRect
import es.pedrazamiguez.splittrip.domain.service.ProfileImageStorageService
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProfileImageStorageServiceImpl(
    private val context: Context
) : ProfileImageStorageService {

    override suspend fun saveAndCompressAvatar(
        userId: String,
        sourceUri: String,
        cropRect: CropRect?
    ): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val avatarsDir = File(context.filesDir, AVATARS_DIR).also { it.mkdirs() }
        val destFile = File(avatarsDir, "$userId.webp")

        try {
            val rotationDegrees = getRotationDegrees(uri, sourceUri)
            val boundsOpts = decodeBounds(uri, sourceUri)
            val inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_IMAGE_DIMENSION)
            val decodedBitmap = decodeOriginalBitmap(uri, sourceUri, inSampleSize)
            val originalBitmap = rotateBitmap(decodedBitmap, rotationDegrees)

            try {
                // 3. Crop
                val croppedBitmap = cropBitmap(originalBitmap, cropRect)
                if (croppedBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                // 4. Downscale to exactly 512x512
                val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, AVATAR_SIZE, AVATAR_SIZE, true)
                if (scaledBitmap != croppedBitmap) {
                    croppedBitmap.recycle()
                }

                // 5. Save as WebP
                FileOutputStream(destFile).use { output ->
                    saveWebp(scaledBitmap, output)
                }
                scaledBitmap.recycle()

                Timber.d("Saved compressed avatar for user $userId to ${destFile.absolutePath}")
                destFile.toUri().toString()
            } catch (e: Exception) {
                if (!originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
                throw e
            }
        } finally {
            cleanUpSourceCameraFile(uri, sourceUri)
        }
    }

    private fun getRotationDegrees(uri: Uri, sourceUri: String): Int {
        val exifStream = openInputStream(uri)
        return exifStream?.use { input ->
            try {
                val exifInterface = ExifInterface(input)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> ROTATION_90
                    ExifInterface.ORIENTATION_ROTATE_180 -> ROTATION_180
                    ExifInterface.ORIENTATION_ROTATE_270 -> ROTATION_270
                    else -> 0
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not read EXIF orientation from $sourceUri")
                0
            }
        } ?: 0
    }

    private fun decodeBounds(uri: Uri, sourceUri: String): BitmapFactory.Options {
        val boundsOpts = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
        val boundsStream = openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open input stream for bounds decoding: $sourceUri")
        boundsStream.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOpts)
        }
        return boundsOpts
    }

    private fun decodeOriginalBitmap(uri: Uri, sourceUri: String, inSampleSize: Int): Bitmap {
        val decodeOpts = BitmapFactory.Options().also { it.inSampleSize = inSampleSize }
        val imageStream = openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open input stream for image decoding: $sourceUri")
        return imageStream.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
        } ?: error("Could not decode image from $sourceUri")
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate decoded bitmap")
            bitmap
        }
    }

    private fun openInputStream(uri: Uri): java.io.InputStream? {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) return stream
        } catch (e: Exception) {
            Timber.e(e, "openInputStream: ContentResolver failed for uri=$uri. Trying local resolution.")
        }

        val resolvedFile = resolveLocalFileFromUri(context, uri)
        if (resolvedFile != null) {
            val fileToOpen = when {
                resolvedFile.exists() -> resolvedFile
                resolvedFile.canonicalFile.exists() -> resolvedFile.canonicalFile
                resolvedFile.absoluteFile.exists() -> resolvedFile.absoluteFile
                else -> null
            }
            if (fileToOpen != null) {
                return try {
                    java.io.FileInputStream(fileToOpen)
                } catch (e: Exception) {
                    Timber.e(
                        e,
                        "openInputStream: Local FileInputStream failed for resolvedFile=${fileToOpen.absolutePath}"
                    )
                    null
                }
            } else {
                val filesDir = context.filesDir
                val avatarsTempDir = File(filesDir, "avatars_temp")
                val filesInTemp = avatarsTempDir.listFiles()?.map { it.name } ?: emptyList()
                val filesInFiles = filesDir.listFiles()?.map { it.name } ?: emptyList()
                Timber.e(
                    "openInputStream: Resolved file does not exist! " +
                        "uri=$uri, " +
                        "resolvedPath=${resolvedFile.absolutePath}, " +
                        "canonicalPath=${resolvedFile.canonicalPath}, " +
                        "filesDir=${filesDir.absolutePath}, " +
                        "avatarsTempDirExists=${avatarsTempDir.exists()}, " +
                        "filesInTempDir=$filesInTemp, " +
                        "filesInFilesDir=$filesInFiles"
                )
            }
        }
        return null
    }

    private fun resolveLocalFileFromUri(context: Context, uri: Uri): File? {
        if (uri.scheme != "content") return null
        val authority = uri.authority ?: return null
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (uri.authority != expectedAuthority && !authority.endsWith(".fileprovider")) return null

        val pathSegments = uri.pathSegments
        if (pathSegments.isEmpty()) return null

        val name = pathSegments[0]
        val remainingPath = pathSegments.drop(1).joinToString("/")

        val filesDir = context.filesDir

        return when (name) {
            "receipts" -> File(filesDir, "receipts/$remainingPath")
            "avatars_temp" -> File(filesDir, "avatars_temp/$remainingPath")
            else -> null
        }
    }

    private fun cropBitmap(originalBitmap: Bitmap, cropRect: CropRect?): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        return if (cropRect != null) {
            val left = (cropRect.left * originalWidth).toInt().coerceIn(0, originalWidth - 1)
            val top = (cropRect.top * originalHeight).toInt().coerceIn(0, originalHeight - 1)
            val right = (cropRect.right * originalWidth).toInt().coerceIn(left + 1, originalWidth)
            val bottom = (cropRect.bottom * originalHeight).toInt().coerceIn(top + 1, originalHeight)
            val cropW = right - left
            val cropH = bottom - top
            Bitmap.createBitmap(originalBitmap, left, top, cropW, cropH)
        } else {
            val size = minOf(originalWidth, originalHeight)
            val left = (originalWidth - size) / 2
            val top = (originalHeight - size) / 2
            Bitmap.createBitmap(originalBitmap, left, top, size, size)
        }
    }

    private fun saveWebp(bitmap: Bitmap, outputStream: FileOutputStream) {
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Bitmap.CompressFormat.valueOf("WEBP_LOSSY")
            } catch (_: Throwable) {
                Bitmap.CompressFormat.WEBP
            }
        } else {
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, WEBP_QUALITY, outputStream)
    }

    override suspend fun deleteLocalAvatar(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val avatarsDir = File(context.filesDir, AVATARS_DIR)
                val file = File(avatarsDir, "$userId.webp")
                if (file.exists()) {
                    if (file.delete()) {
                        Timber.d("Deleted local avatar for user $userId")
                    } else {
                        Timber.w("Failed to delete local avatar file for user $userId")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting local avatar for user $userId")
            }
        }
    }

    override suspend fun cleanTempCameraFiles(): Unit = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.filesDir, "avatars_temp")
            if (!tempDir.exists()) return@withContext

            val files = tempDir.listFiles() ?: return@withContext
            val threshold = System.currentTimeMillis() - CLEANUP_THRESHOLD_MS
            for (file in files) {
                if (file.name.startsWith("avatar_camera_") && file.lastModified() < threshold) {
                    deleteFileQuietly(file)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning leftover camera files")
        }
    }

    private fun cleanUpSourceCameraFile(uri: Uri, sourceUri: String) {
        val authority = uri.authority ?: return
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (authority != expectedAuthority && !authority.endsWith(".fileprovider")) return
        val fileName = uri.lastPathSegment ?: return
        if (!fileName.startsWith("avatar_camera_")) return

        try {
            val tempDir = File(context.filesDir, "avatars_temp")
            val sourceFile = File(tempDir, fileName)
            if (sourceFile.exists()) {
                deleteFileQuietly(sourceFile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clean up source camera temp file: $sourceUri")
        }
    }

    private fun deleteFileQuietly(file: File) {
        if (file.delete()) {
            Timber.d("Deleted temp file: ${file.absolutePath}")
        } else {
            Timber.w("Failed to delete temp file: ${file.absolutePath}")
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        while ((height / inSampleSize) > maxDimension || (width / inSampleSize) > maxDimension) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private companion object {
        const val AVATARS_DIR = "avatars"
        const val AVATAR_SIZE = 512
        const val WEBP_QUALITY = 80
        const val MAX_IMAGE_DIMENSION = 2048
        const val CLEANUP_THRESHOLD_MS = 10 * 60 * 1000L // 10 minutes
        const val ROTATION_90 = 90
        const val ROTATION_180 = 180
        const val ROTATION_270 = 270
    }
}
