package es.pedrazamiguez.splittrip.features.onboarding.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSkipClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingTopHeader(
            isLastStep = uiState.isLastStep,
            onSkipClick = onSkipClick
        )

        OnboardingStepCarousel(
            currentStep = uiState.currentStep,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

        OnboardingStepIndicator(
            totalSteps = uiState.totalSteps,
            currentStepIndex = uiState.currentStepIndex
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

        OnboardingNavigationBar(
            isFirstStep = uiState.isFirstStep,
            isLastStep = uiState.isLastStep,
            onNextClick = onNextClick,
            onPreviousClick = onPreviousClick,
            onCompleteClick = onCompleteClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
    }
}
