package es.pedrazamiguez.splittrip.update

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppUpdateCoordinator {
    val uiState: StateFlow<AppUpdateUiState>
    val actions: SharedFlow<AppUpdateCoordinatorAction>

    fun registerInstallListener(): () -> Unit
    suspend fun checkForUpdates(
        allowOptionalPrompt: Boolean,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    )
    suspend fun onResume(launcher: ActivityResultLauncher<IntentSenderRequest>)
    suspend fun onActivityResult(resultCode: Int)
    suspend fun retryForcedUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>)
    suspend fun completeFlexibleUpdate()
}
