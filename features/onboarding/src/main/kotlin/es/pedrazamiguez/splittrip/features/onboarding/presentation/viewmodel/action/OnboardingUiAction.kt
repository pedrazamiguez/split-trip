package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action

sealed interface OnboardingUiAction {
    data object CompleteOnboarding : OnboardingUiAction
}
