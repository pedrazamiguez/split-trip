package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.OnboardingUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.OnboardingUiEvent
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _actions = Channel<OnboardingUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.NextStep -> onNextStep()
            OnboardingUiEvent.PreviousStep -> onPreviousStep()
            OnboardingUiEvent.Skip -> completeOnboarding()
            OnboardingUiEvent.Complete -> completeOnboarding()
        }
    }

    private fun onNextStep() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentStepIndex
        if (currentIndex < currentState.steps.lastIndex) {
            val nextStep = currentState.steps[currentIndex + 1]
            _uiState.value = currentState.copy(currentStep = nextStep)
        }
    }

    private fun onPreviousStep() {
        val currentState = _uiState.value
        val currentIndex = currentState.currentStepIndex
        if (currentIndex > 0) {
            val previousStep = currentState.steps[currentIndex - 1]
            _uiState.value = currentState.copy(currentStep = previousStep)
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            _actions.send(OnboardingUiAction.CompleteOnboarding)
        }
    }
}
