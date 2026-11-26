package es.pedrazamiguez.splittrip.data.service

import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HttpModelPackDownloaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `rejects non-HTTPS download URLs`() = runTest {
        val downloader = HttpModelPackDownloader()
        val destFile = File(tempDir, "model.litertlm.part")

        val result = downloader.downloadToFile(
            url = "http://example.com/model.litertlm",
            destinationFile = destFile,
            expectedSizeBytes = 100L,
            onProgress = { _, _ -> }
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `streams payload to destination file and reports progress`() = runTest {
        val payload = "model-weights-binary-content".toByteArray()
        val connection = mockk<HttpURLConnection>(relaxed = true) {
            every { responseCode } returns HttpURLConnection.HTTP_OK
            every { contentLengthLong } returns payload.size.toLong()
            every { inputStream } returns ByteArrayInputStream(payload)
        }
        var currentTimeMs = 0L
        val downloader = HttpModelPackDownloader(
            connectionFactory = { connection },
            timeProvider = {
                currentTimeMs += 300L
                currentTimeMs
            }
        )
        val destFile = File(tempDir, "nested/model.litertlm.part")
        val progressEvents = mutableListOf<Long>()

        val result = downloader.downloadToFile(
            url = "https://example.com/model.litertlm",
            destinationFile = destFile,
            expectedSizeBytes = payload.size.toLong(),
            onProgress = { downloaded, _ -> progressEvents.add(downloaded) }
        )

        assertTrue(result.isSuccess)
        assertEquals(payload.size.toLong(), result.getOrThrow())
        assertEquals(payload.toList(), destFile.readBytes().toList())
        assertTrue(progressEvents.isNotEmpty())
        assertEquals(payload.size.toLong(), progressEvents.last())
    }

    @Test
    fun `returns failure on non-200 HTTP status`() = runTest {
        val connection = mockk<HttpURLConnection>(relaxed = true) {
            every { responseCode } returns HttpURLConnection.HTTP_NOT_FOUND
        }
        val downloader = HttpModelPackDownloader(connectionFactory = { connection })
        val destFile = File(tempDir, "model.litertlm.part")

        val result = downloader.downloadToFile(
            url = "https://example.com/missing.litertlm",
            destinationFile = destFile,
            expectedSizeBytes = 100L,
            onProgress = { _, _ -> }
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `returns failure when input stream throws IOException`() = runTest {
        val connection = mockk<HttpURLConnection>(relaxed = true) {
            every { responseCode } returns HttpURLConnection.HTTP_OK
            every { contentLengthLong } returns 100L
            every { inputStream } throws IOException("Connection reset")
        }
        val downloader = HttpModelPackDownloader(connectionFactory = { connection })
        val destFile = File(tempDir, "model.litertlm.part")

        val result = downloader.downloadToFile(
            url = "https://example.com/model.litertlm",
            destinationFile = destFile,
            expectedSizeBytes = 100L,
            onProgress = { _, _ -> }
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
