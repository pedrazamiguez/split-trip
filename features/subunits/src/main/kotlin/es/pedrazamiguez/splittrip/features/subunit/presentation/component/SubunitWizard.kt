package es.pedrazamiguez.splittrip.features.subunit.presentation.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardScaffold
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

@Suppress("LongMethod")
@Composable
internal fun SubunitWizard(
    uiState: CreateEditSubunitUiState,
    onEvent: (CreateEditSubunitUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val nameLabel = stringResource(R.string.subunit_wizard_step_name)
    val membersLabel = stringResource(R.string.subunit_wizard_step_members)
    val sharesLabel = stringResource(R.string.subunit_wizard_step_shares)
    val reviewLabel = stringResource(R.string.subunit_wizard_step_review)

    val stepLabelMap = remember(nameLabel, membersLabel, sharesLabel, reviewLabel) {
        mapOf(
            CreateEditSubunitStep.NAME to nameLabel,
            CreateEditSubunitStep.MEMBERS to membersLabel,
            CreateEditSubunitStep.SHARES to sharesLabel,
            CreateEditSubunitStep.REVIEW to reviewLabel
        )
    }

    val orderedLabels = remember(uiState.steps, stepLabelMap) {
        uiState.steps.map { stepLabelMap[it] ?: "" }
    }

    val backLabel = stringResource(R.string.subunit_wizard_back)
    val nextLabel = stringResource(R.string.subunit_wizard_next)
    val submitLabel = stringResource(R.string.subunit_save)

    WizardScaffold(
        stepLabels = orderedLabels,
        currentStepIndex = uiState.currentStepIndex,
        allowForwardJumps = uiState.isEditing,
        onStepClicked = { onEvent(CreateEditSubunitUiEvent.JumpToStep(it)) },
        navigationConfig = WizardNavigationBarConfig(
            canGoNext = uiState.canGoNext,
            isOnLastStep = uiState.isOnReviewStep,
            isCurrentStepValid = uiState.isCurrentStepValid,
            isLoading = uiState.isSaving,
            backLabel = backLabel,
            nextLabel = nextLabel,
            submitLabel = submitLabel
        ),
        onBack = { onEvent(CreateEditSubunitUiEvent.PreviousStep) },
        onNext = { onEvent(CreateEditSubunitUiEvent.NextStep) },
        onSubmit = { onEvent(CreateEditSubunitUiEvent.Save) },
        modifier = modifier
    ) {
        SubunitWizardStepContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize()
        )
    }
}
