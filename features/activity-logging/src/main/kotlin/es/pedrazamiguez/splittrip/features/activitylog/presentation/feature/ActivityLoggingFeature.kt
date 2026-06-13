package es.pedrazamiguez.splittrip.features.activitylog.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.features.activitylog.presentation.screen.ActivityLoggingScreen
import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.ActivityLoggingViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ActivityLoggingFeature(
    viewModel: ActivityLoggingViewModel = koinViewModel<ActivityLoggingViewModel>()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ActivityLoggingScreen(uiState = uiState)
}
