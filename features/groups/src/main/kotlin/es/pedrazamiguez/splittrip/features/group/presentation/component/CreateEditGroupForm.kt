package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardScaffold
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

@Composable
fun CreateEditGroupForm(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedLabels = RememberGroupStepLabels(uiState.steps)

    WizardScaffold(
        stepLabels = orderedLabels,
        currentStepIndex = uiState.currentStepIndex,
        allowForwardJumps = uiState.isEditMode,
        onStepClicked = { onEvent(CreateEditGroupUiEvent.JumpToStep(it)) },
        navigationConfig = WizardNavigationBarConfig(
            canGoNext = uiState.canGoNext,
            isOnLastStep = uiState.isOnReviewStep,
            isCurrentStepValid = uiState.isCurrentStepValid,
            isLoading = uiState.isLoading,
            backLabel = stringResource(R.string.group_wizard_back),
            nextLabel = stringResource(R.string.group_wizard_next),
            submitLabel = if (uiState.isEditMode) {
                stringResource(R.string.group_edit_save)
            } else {
                stringResource(R.string.groups_create)
            }
        ),
        onBack = { onEvent(CreateEditGroupUiEvent.PreviousStep) },
        onNext = { onEvent(CreateEditGroupUiEvent.NextStep) },
        onSubmit = { onEvent(CreateEditGroupUiEvent.Submit) },
        modifier = modifier
    ) {
        CreateEditGroupWizardContent(
            uiState = uiState,
            onEvent = onEvent,
            onScannerClick = onScannerClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}
