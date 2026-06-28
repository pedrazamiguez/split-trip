package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigator
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateEditGroupNavigationEventHandlerImpl : CreateEditGroupNavigationEventHandler {
    private lateinit var _uiState: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var _actions: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var scope: CoroutineScope
    private val wizardNavigator = WizardNavigator()

    override fun bind(
        stateFlow: MutableStateFlow<CreateEditGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun handleNavigation(event: CreateEditGroupUiEvent) {
        val state = _uiState.value
        when (event) {
            is CreateEditGroupUiEvent.NextStep -> {
                val next = wizardNavigator.navigateNext(state.currentStep, state.steps) ?: return
                _uiState.update { it.copy(currentStep = next, error = null) }
            }
            is CreateEditGroupUiEvent.PreviousStep -> {
                when (val result = wizardNavigator.navigatePrevious(state.currentStep, null, state.steps)) {
                    is WizardNavigator.NavigationResult.WithStep ->
                        _uiState.update { it.copy(currentStep = result.step, error = null) }

                    WizardNavigator.NavigationResult.ExitWizard ->
                        scope.launch { _actions.emit(CreateEditGroupUiAction.NavigateBack) }
                }
            }
            is CreateEditGroupUiEvent.JumpToStep -> {
                val target = wizardNavigator.jumpToStep(state.currentStep, event.stepIndex, state.steps) ?: return
                _uiState.update { it.copy(currentStep = target, error = null) }
            }
            else -> {
                // Ignore non-navigation events
            }
        }
    }
}
