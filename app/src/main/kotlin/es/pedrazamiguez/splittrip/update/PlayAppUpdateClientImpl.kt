package es.pedrazamiguez.splittrip.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestCompleteUpdate
import kotlin.coroutines.cancellation.CancellationException

class PlayAppUpdateClientImpl(
    private val appUpdateManager: AppUpdateManager
) : PlayAppUpdateClient {

    @Volatile
    private var latestAppUpdateInfo: AppUpdateInfo? = null

    override suspend fun fetchUpdateSnapshot(): Result<PlayUpdateSnapshot> {
        return try {
            val info = appUpdateManager.requestAppUpdateInfo()
            latestAppUpdateInfo = info
            Result.success(
                PlayUpdateSnapshot(
                    availability = info.updateAvailability().toPlayUpdateAvailability(),
                    availableVersionCode = info.availableVersionCode().toLong(),
                    isImmediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                    isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                    installStatus = info.installStatus().toPlayInstallStatus()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun startUpdateFlow(
        flowType: PlayUpdateFlowType,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Result<Boolean> {
        val info = latestAppUpdateInfo
            ?: return Result.failure(IllegalStateException("No cached AppUpdateInfo available"))
        val playType = when (flowType) {
            PlayUpdateFlowType.IMMEDIATE -> AppUpdateType.IMMEDIATE
            PlayUpdateFlowType.FLEXIBLE -> AppUpdateType.FLEXIBLE
        }
        return try {
            val started = appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.defaultOptions(playType)
            )
            Result.success(started)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            latestAppUpdateInfo = null
        }
    }

    override suspend fun completeFlexibleUpdate(): Result<Unit> {
        return try {
            appUpdateManager.requestCompleteUpdate()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun registerInstallStatusListener(listener: (PlayInstallStatus) -> Unit): () -> Unit {
        val playListener = InstallStateUpdatedListener { state ->
            listener(state.installStatus().toPlayInstallStatus())
        }
        appUpdateManager.registerListener(playListener)
        return {
            appUpdateManager.unregisterListener(playListener)
        }
    }
}

private fun Int.toPlayUpdateAvailability(): PlayUpdateAvailability = when (this) {
    UpdateAvailability.UPDATE_NOT_AVAILABLE -> PlayUpdateAvailability.UPDATE_NOT_AVAILABLE
    UpdateAvailability.UPDATE_AVAILABLE -> PlayUpdateAvailability.UPDATE_AVAILABLE
    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
        PlayUpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
    else -> PlayUpdateAvailability.UNKNOWN
}

private fun Int.toPlayInstallStatus(): PlayInstallStatus = when (this) {
    InstallStatus.PENDING -> PlayInstallStatus.PENDING
    InstallStatus.DOWNLOADING -> PlayInstallStatus.DOWNLOADING
    InstallStatus.DOWNLOADED -> PlayInstallStatus.DOWNLOADED
    InstallStatus.INSTALLING -> PlayInstallStatus.INSTALLING
    InstallStatus.INSTALLED -> PlayInstallStatus.INSTALLED
    InstallStatus.FAILED -> PlayInstallStatus.FAILED
    InstallStatus.CANCELED -> PlayInstallStatus.CANCELED
    else -> PlayInstallStatus.UNKNOWN
}
