package es.pedrazamiguez.splittrip.features.onboarding.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.permission.checkNotificationPermission
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestNotificationPermission
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.OnboardingScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.OnboardingViewModel
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.OnboardingUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.OnboardingUiEvent
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingFeature(
    onOnboardingComplete: () -> Unit = {},
    viewModel: OnboardingViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val koin = getKoin()
    val telemetryTracker = remember(koin) { koin.get<TelemetryTracker>() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val requestPermission = rememberRequestNotificationPermission { isGranted ->
        viewModel.onEvent(OnboardingUiEvent.UpdateNotificationPermission(isGranted))
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onEvent(
                OnboardingUiEvent.UpdateNotificationPermission(
                    checkNotificationPermission(context)
                )
            )
        }
    }

    LaunchedEffect(viewModel.actions) {
        viewModel.actions.collect { action ->
            when (action) {
                OnboardingUiAction.CompleteOnboarding -> {
                    telemetryTracker.trackEvent("onboarding_complete")
                    onOnboardingComplete()
                }
                OnboardingUiAction.RequestNotificationPermission -> {
                    requestPermission()
                }
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onNextClick = { viewModel.onEvent(OnboardingUiEvent.NextStep) },
        onPreviousClick = { viewModel.onEvent(OnboardingUiEvent.PreviousStep) },
        onSkipClick = { viewModel.onEvent(OnboardingUiEvent.Skip) },
        onCompleteClick = { viewModel.onEvent(OnboardingUiEvent.Complete) },
        onRequestNotificationPermissionClick = {
            viewModel.onEvent(OnboardingUiEvent.RequestNotificationPermission)
        },
        modifier = modifier
    )
}
