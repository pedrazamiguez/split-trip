package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.features.onboarding.presentation.model.OnboardingStep
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.TRIPS_AND_GROUPS,
    val steps: ImmutableList<OnboardingStep> = persistentListOf(
        OnboardingStep.TRIPS_AND_GROUPS,
        OnboardingStep.SMART_SPLITTING,
        OnboardingStep.CASH_AND_POCKET,
        OnboardingStep.CONSENSUS_AND_SETTLEMENT
    )
) {
    val currentStepIndex: Int get() = steps.indexOf(currentStep).coerceAtLeast(0)
    val totalSteps: Int get() = steps.size
    val isFirstStep: Boolean get() = currentStepIndex == 0
    val isLastStep: Boolean get() = currentStepIndex == steps.lastIndex
}
