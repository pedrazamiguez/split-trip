package es.pedrazamiguez.splittrip.update

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState

    data class ForcedUpdateBlocked(
        val availableVersionCode: Long,
        val isRetrying: Boolean = false
    ) : AppUpdateUiState

    data object FlexibleUpdateReadyToInstall : AppUpdateUiState
}

sealed interface AppUpdateCoordinatorAction {
    data object OpenPlayStoreListing : AppUpdateCoordinatorAction
}
