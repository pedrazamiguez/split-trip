package es.pedrazamiguez.splittrip.data.service

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal interface ModelPackDownloader {
    suspend fun downloadToFile(
        url: String,
        destinationFile: File,
        expectedSizeBytes: Long,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Long>
}

internal class HttpModelPackDownloader(
    private val connectionFactory: (URL) -> HttpURLConnection = { targetUrl ->
        targetUrl.openConnection() as HttpURLConnection
    },
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : ModelPackDownloader {

    override suspend fun downloadToFile(
        url: String,
        destinationFile: File,
        expectedSizeBytes: Long,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<Long> {
        if (!url.startsWith(HTTPS_SCHEME, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Model download URL must use HTTPS"))
        }

        val parentDir = destinationFile.parentFile
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            return Result.failure(IOException("Unable to create model storage directory"))
        }

        var connection: HttpURLConnection? = null
        return try {
            connection = connectionFactory(URL(url)).apply {
                requestMethod = HTTP_GET
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(IOException("Unexpected HTTP response code: $responseCode"))
            }

            val contentLength = connection.contentLengthLong.takeIf { it > 0L } ?: expectedSizeBytes
            val totalDownloaded = streamToFile(
                connection = connection,
                destinationFile = destinationFile,
                totalBytes = contentLength,
                onProgress = onProgress
            )
            Result.success(totalDownloaded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun streamToFile(
        connection: HttpURLConnection,
        destinationFile: File,
        totalBytes: Long,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Long {
        var downloadedBytes = 0L
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(destinationFile).use { output ->
                downloadedBytes = copyChunks(
                    input = input,
                    output = output,
                    totalBytes = totalBytes,
                    onProgress = onProgress
                )
                output.flush()
            }
        }
        onProgress(downloadedBytes, totalBytes)
        return downloadedBytes
    }

    private suspend fun copyChunks(
        input: BufferedInputStream,
        output: FileOutputStream,
        totalBytes: Long,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Long {
        var downloadedBytes = 0L
        var lastReportedPercent = -1
        var lastReportTimeMs = 0L
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        var bytesRead = input.read(buffer)
        while (bytesRead != -1) {
            currentCoroutineContext().ensureActive()
            output.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead
            val currentPercent = computeProgressPercent(downloadedBytes, totalBytes)
            val now = timeProvider()
            if (shouldReportProgress(currentPercent - lastReportedPercent, now - lastReportTimeMs)) {
                lastReportedPercent = currentPercent
                lastReportTimeMs = now
                onProgress(downloadedBytes, totalBytes)
            }
            bytesRead = input.read(buffer)
        }
        return downloadedBytes
    }

    private fun shouldReportProgress(percentDelta: Int, timeDeltaMs: Long): Boolean =
        percentDelta >= PROGRESS_MIN_PERCENT_DELTA || timeDeltaMs >= PROGRESS_THROTTLE_MS

    private fun computeProgressPercent(downloadedBytes: Long, totalBytes: Long): Int {
        if (totalBytes <= 0L) return 0
        return ((downloadedBytes * PERCENT_SCALE) / totalBytes).toInt().coerceIn(0, PERCENT_SCALE)
    }

    private companion object {
        private const val HTTPS_SCHEME = "https://"
        private const val HTTP_GET = "GET"
        private const val TIMEOUT_MS = 30_000
        private const val STREAM_BUFFER_SIZE = 32_768
        private const val PERCENT_SCALE = 100
        private const val PROGRESS_MIN_PERCENT_DELTA = 1
        private const val PROGRESS_THROTTLE_MS = 250L
    }
}
