package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
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
    val bottomPadding = LocalBottomPadding.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            WizardStepIndicator(
                stepLabels = orderedLabels,
                currentStepIndex = uiState.currentStepIndex,
                onStepClicked = { onEvent(CreateEditGroupUiEvent.JumpToStep(it)) }
            )
            CreateEditGroupWizardContent(
                uiState = uiState,
                onEvent = onEvent,
                onScannerClick = onScannerClick,
                modifier = Modifier.weight(1f)
            )
        }

        WizardNavigationBar(
            config = WizardNavigationBarConfig(
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
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}
