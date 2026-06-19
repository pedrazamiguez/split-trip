package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigator
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupNavigationEventHandlerImpl : CreateGroupNavigationEventHandler {
    private lateinit var _uiState: MutableStateFlow<CreateGroupUiState>
    private lateinit var _actions: MutableSharedFlow<CreateGroupUiAction>
    private lateinit var scope: CoroutineScope
    private val wizardNavigator = WizardNavigator()

    override fun bind(
        stateFlow: MutableStateFlow<CreateGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateGroupUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun handleNavigation(event: CreateGroupUiEvent) {
        val state = _uiState.value
        when (event) {
            is CreateGroupUiEvent.NextStep -> {
                val next = wizardNavigator.navigateNext(state.currentStep, state.steps) ?: return
                _uiState.update { it.copy(currentStep = next, error = null) }
            }
            is CreateGroupUiEvent.PreviousStep -> {
                when (val result = wizardNavigator.navigatePrevious(state.currentStep, null, state.steps)) {
                    is WizardNavigator.NavigationResult.WithStep ->
                        _uiState.update { it.copy(currentStep = result.step, error = null) }

                    WizardNavigator.NavigationResult.ExitWizard ->
                        scope.launch { _actions.emit(CreateGroupUiAction.NavigateBack) }
                }
            }
            is CreateGroupUiEvent.JumpToStep -> {
                val target = wizardNavigator.jumpToStep(state.currentStep, event.stepIndex, state.steps) ?: return
                _uiState.update { it.copy(currentStep = target, error = null) }
            }
            else -> {
                // Ignore non-navigation events in the navigation handler
            }
        }
    }
}
