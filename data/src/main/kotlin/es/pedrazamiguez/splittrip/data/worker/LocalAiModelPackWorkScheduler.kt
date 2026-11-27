package es.pedrazamiguez.splittrip.data.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal sealed interface ModelPackWorkState {
    data object Idle : ModelPackWorkState
    data class Queued(val packId: String = "") : ModelPackWorkState
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val packId: String = ""
    ) : ModelPackWorkState

    data class Verifying(
        val version: String,
        val packId: String = ""
    ) : ModelPackWorkState

    data class Succeeded(val packId: String = "") : ModelPackWorkState
    data class Failed(
        val reason: ModelPackErrorReason,
        val packId: String = ""
    ) : ModelPackWorkState

    data object Cancelled : ModelPackWorkState
}

internal interface LocalAiModelPackWorkScheduler {
    fun observeWorkState(): Flow<ModelPackWorkState>
    fun enqueueDownload(config: ModelPackConfig, wifiOnly: Boolean)
    fun cancelDownload()
}

internal class WorkManagerLocalAiModelPackScheduler(
    private val workManager: WorkManager
) : LocalAiModelPackWorkScheduler {

    override fun observeWorkState(): Flow<ModelPackWorkState> =
        workManager.getWorkInfosForUniqueWorkFlow(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME)
            .map { workInfos -> mapWorkInfoList(workInfos) }

    override fun enqueueDownload(config: ModelPackConfig, wifiOnly: Boolean) {
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            LocalAiModelDownloadWorker.KEY_PACK_ID to config.packId,
            LocalAiModelDownloadWorker.KEY_VERSION to config.version,
            LocalAiModelDownloadWorker.KEY_DOWNLOAD_URL to config.downloadUrl,
            LocalAiModelDownloadWorker.KEY_SHA256 to config.sha256Checksum,
            LocalAiModelDownloadWorker.KEY_SIZE_BYTES to config.sizeBytes
        )

        val request = OneTimeWorkRequestBuilder<LocalAiModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("$PACK_ID_TAG_PREFIX${config.packId}")
            .build()

        workManager.enqueueUniqueWork(
            LocalAiModelDownloadWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancelDownload() {
        workManager.cancelUniqueWork(LocalAiModelDownloadWorker.UNIQUE_WORK_NAME)
        workManager.pruneWork()
    }

    private fun mapWorkInfoList(workInfos: List<WorkInfo>): ModelPackWorkState {
        val info = workInfos.firstOrNull() ?: return ModelPackWorkState.Idle
        val packId = resolvePackId(info)
        return when (info.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED -> ModelPackWorkState.Queued(packId = packId)

            WorkInfo.State.RUNNING -> mapRunningWorkInfo(info, packId)
            WorkInfo.State.SUCCEEDED -> ModelPackWorkState.Succeeded(packId = packId)
            WorkInfo.State.FAILED -> {
                val reasonName = info.outputData.getString(LocalAiModelDownloadWorker.KEY_ERROR_REASON)
                val reason = reasonName?.let {
                    runCatching { ModelPackErrorReason.valueOf(it) }.getOrNull()
                } ?: ModelPackErrorReason.UNKNOWN
                ModelPackWorkState.Failed(reason = reason, packId = packId)
            }

            WorkInfo.State.CANCELLED -> ModelPackWorkState.Cancelled
        }
    }

    private fun mapRunningWorkInfo(info: WorkInfo, packId: String): ModelPackWorkState {
        val stage = info.progress.getString(LocalAiModelDownloadWorker.KEY_STAGE)
        if (stage == LocalAiModelDownloadWorker.STAGE_VERIFYING) {
            val version = info.progress.getString(LocalAiModelDownloadWorker.KEY_VERSION).orEmpty()
            return ModelPackWorkState.Verifying(version = version, packId = packId)
        }
        val percent = info.progress.getInt(LocalAiModelDownloadWorker.KEY_PROGRESS_PERCENT, 0)
        val downloaded = info.progress.getLong(LocalAiModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0L)
        val total = info.progress.getLong(LocalAiModelDownloadWorker.KEY_TOTAL_BYTES, 0L)
        return ModelPackWorkState.Downloading(
            progressPercent = percent,
            downloadedBytes = downloaded,
            totalBytes = total,
            packId = packId
        )
    }

    private fun resolvePackId(info: WorkInfo): String =
        info.progress.getString(LocalAiModelDownloadWorker.KEY_PACK_ID)?.takeIf { it.isNotBlank() }
            ?: info.outputData.getString(LocalAiModelDownloadWorker.KEY_PACK_ID)?.takeIf { it.isNotBlank() }
            ?: info.tags.firstOrNull { it.startsWith(PACK_ID_TAG_PREFIX) }
                ?.removePrefix(PACK_ID_TAG_PREFIX)
                .orEmpty()

    private companion object {
        private const val PACK_ID_TAG_PREFIX = "pack_id:"
    }
}
