package es.pedrazamiguez.splittrip.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

interface PlayAppUpdateClient {
    suspend fun fetchUpdateSnapshot(): Result<PlayUpdateSnapshot>
    fun startUpdateFlow(
        flowType: PlayUpdateFlowType,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Result<Boolean>
    suspend fun completeFlexibleUpdate(): Result<Unit>
    fun registerInstallStatusListener(listener: (PlayInstallState) -> Unit): () -> Unit
}
