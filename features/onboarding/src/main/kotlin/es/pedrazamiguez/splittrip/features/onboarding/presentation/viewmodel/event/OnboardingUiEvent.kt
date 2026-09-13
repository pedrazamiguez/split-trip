package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event

sealed interface OnboardingUiEvent {
    data object NextStep : OnboardingUiEvent
    data object PreviousStep : OnboardingUiEvent
    data object Skip : OnboardingUiEvent
    data object Complete : OnboardingUiEvent
}
