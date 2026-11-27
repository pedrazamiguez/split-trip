package es.pedrazamiguez.splittrip.data.service

import io.mockk.every
import io.mockk.spyk
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ModelPackStorageManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `computeSha256 returns exact lowercase hex digest for known payload`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val file = storageManager.getTempFile("pack-1", "1.0.0")
        file.parentFile?.mkdirs()
        file.writeText("abc")

        val digest = storageManager.computeSha256(file)

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            digest
        )
    }

    @Test
    fun `promoteTempToFinal moves part file to target path and replaces existing file`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val tempFile = storageManager.getTempFile("pack-1", "1.0.0")
        val targetFile = storageManager.getTargetFile("pack-1", "1.0.0")
        tempFile.parentFile?.mkdirs()
        targetFile.writeText("old-weights")
        tempFile.writeText("new-weights")

        val promoted = storageManager.promoteTempToFinal(tempFile, targetFile)

        assertTrue(promoted)
        assertFalse(tempFile.exists())
        assertTrue(targetFile.exists())
        assertEquals("new-weights", targetFile.readText())
    }

    @Test
    fun `isFileValid returns true only when file exists and matches expected byte size`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val targetFile = storageManager.getTargetFile("pack-1", "1.0.0")
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        assertTrue(storageManager.isFileValid(targetFile.absolutePath, 4L))
        assertFalse(storageManager.isFileValid(targetFile.absolutePath, 5L))
        assertFalse(storageManager.isFileValid(targetFile.absolutePath, 0L))
        assertFalse(storageManager.isFileValid(null, 4L))
        assertFalse(storageManager.isFileValid(File(tempDir, "missing.litertlm").absolutePath, 4L))
    }

    @Test
    fun `deleteAllModelFiles removes nested pack directories and partial files`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val targetFile = storageManager.getTargetFile("pack-1", "1.0.0")
        val tempFile = storageManager.getTempFile("pack-1", "2.0.0")
        targetFile.parentFile?.mkdirs()
        tempFile.parentFile?.mkdirs()
        targetFile.writeText("weights")
        tempFile.writeText("partial")

        val deleted = storageManager.deleteAllModelFiles()

        assertTrue(deleted)
        assertFalse(targetFile.exists())
        assertFalse(tempFile.exists())
    }

    @Test
    fun `deleteTempFiles removes existing part file and ignores missing file`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val tempFile = storageManager.getTempFile("pack-1", "1.0.0")
        tempFile.parentFile?.mkdirs()
        tempFile.writeText("partial")

        storageManager.deleteTempFiles("pack-1", "1.0.0")
        assertFalse(tempFile.exists())

        storageManager.deleteTempFiles("pack-1", "1.0.0")
        assertFalse(tempFile.exists())
    }

    @Test
    fun `promoteTempToFinal returns false when temp file is missing and deleteAllModelFiles succeeds on empty dir`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val missingTemp = storageManager.getTempFile("pack-1", "1.0.0")
        val targetFile = storageManager.getTargetFile("pack-1", "1.0.0")

        assertFalse(storageManager.promoteTempToFinal(missingTemp, targetFile))
        assertTrue(storageManager.deleteAllModelFiles())
    }

    @Test
    fun `promoteTempToFinal falls back to copyTo when renameTo fails even if temp delete returns false`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val realTemp = storageManager.getTempFile("pack-1", "1.0.0")
        val targetFile = storageManager.getTargetFile("pack-1", "1.0.0")
        realTemp.parentFile?.mkdirs()
        realTemp.writeText("fallback-weights")

        val spiedTemp = spyk(realTemp) {
            every { renameTo(any()) } returns false
            every { delete() } returns false
        }

        val promoted = storageManager.promoteTempToFinal(spiedTemp, targetFile)

        assertTrue(promoted)
        assertTrue(targetFile.exists())
        assertEquals("fallback-weights", targetFile.readText())
    }

    @Test
    fun `deleteOtherModelFiles removes other pack directories and older versions while preserving kept pack file`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val keptTarget = storageManager.getTargetFile("qwen2.5-1.5b-litert-v1", "2.0.0")
        val keptLingeringTemp = storageManager.getTempFile("qwen2.5-1.5b-litert-v1", "2.0.0")
        val oldVersionTarget = storageManager.getTargetFile("qwen2.5-1.5b-litert-v1", "1.0.0")
        val otherPackTarget = storageManager.getTargetFile("qwen3-0.6b-litert-v1", "1.0.0")
        listOf(keptTarget, keptLingeringTemp, oldVersionTarget, otherPackTarget).forEach { file ->
            file.parentFile?.mkdirs()
            file.writeText(file.name)
        }

        storageManager.deleteOtherModelFiles(keepPackId = "qwen2.5-1.5b-litert-v1", keepVersion = "2.0.0")

        assertTrue(keptTarget.exists())
        assertFalse(keptLingeringTemp.exists())
        assertFalse(oldVersionTarget.exists())
        assertFalse(otherPackTarget.exists())
    }

    @Test
    fun `deleteAllTempFiles removes all part files across pack directories without deleting installed model files`() {
        val storageManager = ModelPackStorageManager(tempDir)
        val installedTarget = storageManager.getTargetFile("qwen3-0.6b-litert-v1", "1.0.0")
        val tempA = storageManager.getTempFile("qwen3-0.6b-litert-v1", "1.0.0")
        val tempB = storageManager.getTempFile("gemma4-e2b-litert-v1", "1.0.0")
        listOf(installedTarget, tempA, tempB).forEach { file ->
            file.parentFile?.mkdirs()
            file.writeText(file.name)
        }

        storageManager.deleteAllTempFiles()

        assertTrue(installedTarget.exists())
        assertFalse(tempA.exists())
        assertFalse(tempB.exists())
    }
}
