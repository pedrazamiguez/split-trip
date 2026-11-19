package es.pedrazamiguez.splittrip.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.AppUpdateGate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.AppUpdateReadyBanner
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.update.AppUpdateCoordinator
import es.pedrazamiguez.splittrip.update.AppUpdateCoordinatorAction
import es.pedrazamiguez.splittrip.update.AppUpdateUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
internal fun AppUpdateEffect(
    coordinator: AppUpdateCoordinator,
    isNavHostReady: Boolean,
    isOnMainDestination: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by coordinator.uiState.collectAsStateWithLifecycle()

    val updateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        coroutineScope.launch {
            coordinator.onActivityResult(result.resultCode)
        }
    }

    DisposableEffect(coordinator) {
        val unregister = coordinator.registerInstallListener()
        onDispose { unregister() }
    }

    DisposableEffect(lifecycleOwner, coordinator) {
        var resumeJob: Job? = null
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && resumeJob?.isActive != true) {
                resumeJob = coroutineScope.launch {
                    coordinator.onResume(updateLauncher)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            resumeJob?.cancel()
        }
    }

    LaunchedEffect(coordinator, isOnMainDestination) {
        coordinator.checkForUpdates(
            allowOptionalPrompt = isOnMainDestination,
            launcher = updateLauncher
        )
    }

    LaunchedEffect(coordinator, context) {
        coordinator.actions.collect { action ->
            when (action) {
                AppUpdateCoordinatorAction.OpenPlayStoreListing -> openPlayStoreListing(context)
            }
        }
    }

    AppUpdateOverlayContent(
        uiState = uiState,
        isNavHostReady = isNavHostReady,
        onRestartClick = { coroutineScope.launch { coordinator.completeFlexibleUpdate() } },
        onRetryForcedUpdate = { coroutineScope.launch { coordinator.retryForcedUpdate(updateLauncher) } }
    )
}

@Composable
private fun AppUpdateOverlayContent(
    uiState: AppUpdateUiState,
    isNavHostReady: Boolean,
    onRestartClick: () -> Unit,
    onRetryForcedUpdate: () -> Unit
) {
    AppUpdateReadyBanner(
        visible = isNavHostReady && uiState is AppUpdateUiState.FlexibleUpdateReadyToInstall,
        onRestartClick = onRestartClick
    )

    val forcedState = uiState as? AppUpdateUiState.ForcedUpdateBlocked
    if (forcedState != null) {
        AppUpdateGate(
            isUpdating = forcedState.isRetrying,
            onUpdateClick = onRetryForcedUpdate
        )
    }
}

private fun openPlayStoreListing(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(webIntent)
        } catch (webError: ActivityNotFoundException) {
            Timber.tag(LogTag.APP_UPDATE).w(webError, "Unable to open Play Store listing")
        }
    }
}
