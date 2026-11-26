package es.pedrazamiguez.splittrip.data.service

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

internal class ModelPackStorageManager(baseDir: File) {

    private val modelsRoot = File(baseDir, MODELS_DIR_NAME)

    fun getTargetFile(packId: String, version: String): File =
        File(getVersionDir(packId, version), MODEL_FILE_NAME)

    fun getTempFile(packId: String, version: String): File =
        File(getVersionDir(packId, version), TEMP_MODEL_FILE_NAME)

    fun isFileValid(path: String?, expectedSizeBytes: Long): Boolean {
        if (path.isNullOrBlank() || expectedSizeBytes <= 0L) return false
        val file = File(path)
        return file.exists() && file.isFile && file.length() == expectedSizeBytes
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance(SHA_256_ALGORITHM)
        val buffer = ByteArray(HASH_BUFFER_SIZE)
        FileInputStream(file).use { input ->
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    fun promoteTempToFinal(tempFile: File, targetFile: File): Boolean {
        if (!tempFile.exists() || !tempFile.isFile) return false
        val parent = targetFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false
        if (targetFile.exists() && !targetFile.delete()) return false
        if (tempFile.renameTo(targetFile)) return true
        return try {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteTempFiles(packId: String, version: String) {
        val tempFile = getTempFile(packId, version)
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    fun deleteAllModelFiles(): Boolean {
        if (!modelsRoot.exists()) return true
        return modelsRoot.deleteRecursively()
    }

    private fun getVersionDir(packId: String, version: String): File {
        val safePackId = packId.replace(INVALID_SEGMENT_REGEX, "_")
        val safeVersion = version.replace(INVALID_SEGMENT_REGEX, "_")
        return File(File(modelsRoot, safePackId), safeVersion)
    }

    private companion object {
        private const val MODELS_DIR_NAME = "ai_models"
        private const val MODEL_FILE_NAME = "model.litertlm"
        private const val TEMP_MODEL_FILE_NAME = "model.litertlm.part"
        private const val SHA_256_ALGORITHM = "SHA-256"
        private const val HASH_BUFFER_SIZE = 8_192
        private val INVALID_SEGMENT_REGEX = Regex("[^A-Za-z0-9._-]")
    }
}
