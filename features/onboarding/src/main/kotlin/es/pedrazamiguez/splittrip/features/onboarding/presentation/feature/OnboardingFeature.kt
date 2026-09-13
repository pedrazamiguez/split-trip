package es.pedrazamiguez.splittrip.features.onboarding.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(viewModel.actions) {
        viewModel.actions.collect { action ->
            when (action) {
                OnboardingUiAction.CompleteOnboarding -> {
                    telemetryTracker.trackEvent("onboarding_complete")
                    onOnboardingComplete()
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
        modifier = modifier
    )
}
