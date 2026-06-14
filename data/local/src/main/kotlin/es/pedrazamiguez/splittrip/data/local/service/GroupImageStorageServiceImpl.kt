package es.pedrazamiguez.splittrip.data.local.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GroupImageStorageServiceImpl(
    private val context: Context
) : GroupImageStorageService {

    override suspend fun saveTempGroupImage(sourceUri: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(sourceUri)
        val tempDir = File(context.filesDir, TEMP_DIR).also { it.mkdirs() }
        val destFile = File(tempDir, "group_temp_${System.currentTimeMillis()}.webp")
        var decodedBitmap: Bitmap? = null
        var rotatedBitmap: Bitmap? = null

        try {
            val rotationDegrees = getRotationDegrees(uri, sourceUri)
            val boundsOpts = decodeBounds(uri, sourceUri)
            val inSampleSize = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, MAX_IMAGE_DIMENSION)
            decodedBitmap = decodeOriginalBitmap(uri, sourceUri, inSampleSize)
            rotatedBitmap = rotateBitmap(decodedBitmap, rotationDegrees)

            FileOutputStream(destFile).use { output ->
                saveWebp(rotatedBitmap, output)
            }

            Timber.d("Saved temp group image to ${destFile.absolutePath}")
            destFile.toUri().toString()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save temp group image from $sourceUri")
            throw e
        } finally {
            rotatedBitmap?.recycle()
            if (decodedBitmap != null && decodedBitmap != rotatedBitmap && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
            cleanUpSourceCameraFile(uri, sourceUri)
        }
    }

    override suspend fun commitGroupImage(groupId: String, tempUri: String): String = withContext(Dispatchers.IO) {
        val sourceUri = Uri.parse(tempUri)
        val sourceFile = if (sourceUri.scheme == "file") {
            File(sourceUri.path ?: error("Invalid path for file URI: $tempUri"))
        } else {
            resolveLocalFileFromUri(context, sourceUri) ?: error("Could not resolve local file for URI: $tempUri")
        }

        if (!sourceFile.exists()) {
            error("Source temp file does not exist: ${sourceFile.absolutePath}")
        }

        val groupsDir = File(context.filesDir, GROUPS_DIR).also { it.mkdirs() }
        val destFile = File(groupsDir, "$groupId.webp")

        if (sourceFile.renameTo(destFile)) {
            Timber.d("Successfully moved temp group image to permanent storage: ${destFile.absolutePath}")
        } else {
            sourceFile.copyTo(destFile, overwrite = true)
            sourceFile.delete()
            Timber.d(
                "Successfully copied temp group image to permanent storage and deleted temp: ${destFile.absolutePath}"
            )
        }

        destFile.toUri().toString()
    }

    override suspend fun deleteLocalGroupImage(groupId: String): Unit = withContext(Dispatchers.IO) {
        try {
            val groupsDir = File(context.filesDir, GROUPS_DIR)
            val file = File(groupsDir, "$groupId.webp")
            if (file.exists()) {
                if (file.delete()) {
                    Timber.d("Deleted local group image for group $groupId")
                } else {
                    Timber.w("Failed to delete local group image file for group $groupId")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting local group image for group $groupId")
        }
    }

    override suspend fun cleanTempGroupImages(): Unit = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.filesDir, TEMP_DIR)
            if (!tempDir.exists()) return@withContext

            val files = tempDir.listFiles() ?: return@withContext
            val threshold = System.currentTimeMillis() - CLEANUP_THRESHOLD_MS
            for (file in files) {
                if ((file.name.startsWith("group_temp_") || file.name.startsWith("group_camera_")) &&
                    file.lastModified() < threshold
                ) {
                    deleteFileQuietly(file)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning leftover group temp images")
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
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
            Timber.e(e, "openInputStream ContentResolver failed for uri=$uri. Trying local resolution.")
        }

        val resolvedFile = resolveLocalFileFromUri(context, uri)
        if (resolvedFile != null && resolvedFile.exists()) {
            return try {
                java.io.FileInputStream(resolvedFile)
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "openInputStream: Local FileInputStream failed for resolvedFile=${resolvedFile.absolutePath}"
                )
                null
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

        return when (name) {
            "groups_temp" -> File(context.filesDir, "groups_temp/$remainingPath")
            else -> null
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

    private fun cleanUpSourceCameraFile(uri: Uri, sourceUri: String) {
        val authority = uri.authority ?: return
        val expectedAuthority = "${context.packageName}.fileprovider"
        if (authority != expectedAuthority && !authority.endsWith(".fileprovider")) return
        val fileName = uri.lastPathSegment ?: return
        if (!fileName.startsWith("group_camera_")) return

        try {
            val tempDir = File(context.filesDir, TEMP_DIR)
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
        const val GROUPS_DIR = "groups"
        const val TEMP_DIR = "groups_temp"
        const val WEBP_QUALITY = 80
        const val MAX_IMAGE_DIMENSION = 2048
        const val CLEANUP_THRESHOLD_MS = 10 * 60 * 1000L // 10 minutes
        const val ROTATION_90 = 90
        const val ROTATION_180 = 180
        const val ROTATION_270 = 270
    }
}
