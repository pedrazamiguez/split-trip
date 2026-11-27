package es.pedrazamiguez.splittrip.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import es.pedrazamiguez.splittrip.data.litert.LiteRtEngineClient
import es.pedrazamiguez.splittrip.data.local.datastore.InstalledModelPackMetadata
import es.pedrazamiguez.splittrip.data.local.datastore.LocalAiModelPackPreferences
import es.pedrazamiguez.splittrip.data.service.ModelPackDownloader
import es.pedrazamiguez.splittrip.data.service.ModelPackStorageManager
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import timber.log.Timber

class LocalAiModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Suppress("LongMethod", "ReturnCount")
    override suspend fun doWork(): Result {
        val packId = inputData.getString(KEY_PACK_ID).orEmpty()
        val version = inputData.getString(KEY_VERSION).orEmpty()
        val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL).orEmpty()
        val expectedSha256 = inputData.getString(KEY_SHA256).orEmpty()
        val expectedSizeBytes = inputData.getLong(KEY_SIZE_BYTES, 0L)

        if (listOf(packId, version, downloadUrl, expectedSha256).any { it.isBlank() }) {
            return failureResult(ModelPackErrorReason.INVALID_CONFIG, packId)
        }

        val koin = GlobalContext.get()
        val downloader: ModelPackDownloader = koin.get()
        val storageManager: ModelPackStorageManager = koin.get()
        val preferences: LocalAiModelPackPreferences = koin.get()

        val tempFile = storageManager.getTempFile(packId, version)
        val targetFile = storageManager.getTargetFile(packId, version)

        return try {
            setProgress(
                workDataOf(
                    KEY_PACK_ID to packId,
                    KEY_STAGE to STAGE_DOWNLOADING,
                    KEY_PROGRESS_PERCENT to 0,
                    KEY_DOWNLOADED_BYTES to 0L,
                    KEY_TOTAL_BYTES to expectedSizeBytes
                )
            )

            val downloadedBytes = downloader.downloadToFile(
                url = downloadUrl,
                destinationFile = tempFile,
                expectedSizeBytes = expectedSizeBytes
            ) { downloaded, total ->
                val percent = if (total > 0L) {
                    ((downloaded * PERCENT_SCALE) / total).toInt().coerceIn(0, PERCENT_SCALE)
                } else {
                    0
                }
                setProgress(
                    workDataOf(
                        KEY_PACK_ID to packId,
                        KEY_STAGE to STAGE_DOWNLOADING,
                        KEY_PROGRESS_PERCENT to percent,
                        KEY_DOWNLOADED_BYTES to downloaded,
                        KEY_TOTAL_BYTES to total
                    )
                )
            }.getOrThrow()

            setProgress(
                workDataOf(
                    KEY_PACK_ID to packId,
                    KEY_STAGE to STAGE_VERIFYING,
                    KEY_VERSION to version
                )
            )

            val actualSha256 = storageManager.computeSha256(tempFile)
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                Timber.e("LocalAiModelDownloadWorker: SHA-256 checksum mismatch for %s", packId)
                storageManager.deleteTempFiles(packId, version)
                return failureResult(ModelPackErrorReason.CHECKSUM_MISMATCH, packId)
            }

            val previousInstalled = preferences.installedPackMetadata.first()
            if (!storageManager.promoteTempToFinal(tempFile, targetFile)) {
                Timber.e("LocalAiModelDownloadWorker: Failed to promote temp file for %s", packId)
                storageManager.deleteTempFiles(packId, version)
                return failureResult(ModelPackErrorReason.IO_ERROR, packId)
            }

            preferences.saveInstalledPackMetadata(
                InstalledModelPackMetadata(
                    packId = packId,
                    version = version,
                    sizeBytes = downloadedBytes,
                    sha256Checksum = actualSha256,
                    filePath = targetFile.absolutePath
                )
            )
            val previousPath = previousInstalled?.filePath
            if (!previousPath.isNullOrBlank() && previousPath != targetFile.absolutePath) {
                val engineClient: LiteRtEngineClient = koin.get()
                engineClient.invalidate(previousPath)
            }
            storageManager.deleteOtherModelFiles(keepPackId = packId, keepVersion = version)
            Result.success(workDataOf(KEY_PACK_ID to packId))
        } catch (e: CancellationException) {
            storageManager.deleteTempFiles(packId, version)
            throw e
        } catch (e: IOException) {
            Timber.e(e, "LocalAiModelDownloadWorker: I/O error downloading %s", packId)
            storageManager.deleteTempFiles(packId, version)
            val reason = if (isStorageFullError(e)) {
                ModelPackErrorReason.INSUFFICIENT_STORAGE
            } else {
                ModelPackErrorReason.NETWORK_ERROR
            }
            failureResult(reason, packId)
        } catch (e: Exception) {
            Timber.e(e, "LocalAiModelDownloadWorker: Unexpected error downloading %s", packId)
            storageManager.deleteTempFiles(packId, version)
            failureResult(ModelPackErrorReason.UNKNOWN, packId)
        }
    }

    private fun isStorageFullError(error: IOException): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("enospc") || message.contains("no space")
    }

    private fun failureResult(reason: ModelPackErrorReason, packId: String): Result =
        Result.failure(
            workDataOf(
                KEY_ERROR_REASON to reason.name,
                KEY_PACK_ID to packId
            )
        )

    companion object {
        const val UNIQUE_WORK_NAME = "local_ai_model_pack_download"
        const val KEY_PACK_ID = "pack_id"
        const val KEY_VERSION = "version"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_SHA256 = "sha256"
        const val KEY_SIZE_BYTES = "size_bytes"
        const val KEY_STAGE = "stage"
        const val KEY_PROGRESS_PERCENT = "progress_percent"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR_REASON = "error_reason"
        const val STAGE_DOWNLOADING = "downloading"
        const val STAGE_VERIFYING = "verifying"
        private const val PERCENT_SCALE = 100
    }
}
