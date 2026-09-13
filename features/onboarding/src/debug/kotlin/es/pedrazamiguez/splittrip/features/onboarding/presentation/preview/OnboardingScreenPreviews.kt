package es.pedrazamiguez.splittrip.features.onboarding.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocales
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.OnboardingScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState

@PreviewThemes
@PreviewLocales
@Composable
private fun OnboardingScreenFirstStepPreview() {
    PreviewThemeWrapper {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentStep = OnboardingStep.TRIPS_AND_GROUPS
            )
        )
    }
}

@PreviewThemes
@Composable
private fun OnboardingScreenIntermediateStepPreview() {
    PreviewThemeWrapper {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentStep = OnboardingStep.SMART_SPLITTING
            )
        )
    }
}

@PreviewThemes
@Composable
private fun OnboardingScreenFinalStepPreview() {
    PreviewThemeWrapper {
        OnboardingScreen(
            uiState = OnboardingUiState(
                currentStep = OnboardingStep.CONSENSUS_AND_SETTLEMENT
            )
        )
    }
}
