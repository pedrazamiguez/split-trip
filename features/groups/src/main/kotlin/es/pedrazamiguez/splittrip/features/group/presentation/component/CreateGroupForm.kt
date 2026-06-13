package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupStep
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

@Composable
fun CreateGroupForm(
    uiState: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
    onScannerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infoLabel = stringResource(R.string.group_wizard_step_info)
    val currencyLabel = stringResource(R.string.group_wizard_step_currency)
    val membersLabel = stringResource(R.string.group_wizard_step_members)
    val reviewLabel = stringResource(R.string.group_wizard_step_review)

    val stepLabels = remember(infoLabel, currencyLabel, membersLabel, reviewLabel) {
        mapOf(
            CreateGroupStep.INFO to infoLabel,
            CreateGroupStep.CURRENCY to currencyLabel,
            CreateGroupStep.MEMBERS to membersLabel,
            CreateGroupStep.REVIEW to reviewLabel
        )
    }

    val orderedLabels = remember(uiState.steps, stepLabels) {
        uiState.steps.map { stepLabels[it] ?: "" }
    }

    val bottomPadding = LocalBottomPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WizardStepIndicator(
                stepLabels = orderedLabels,
                currentStepIndex = uiState.currentStepIndex,
                onStepClicked = { onEvent(CreateGroupUiEvent.JumpToStep(it)) }
            )

            CreateGroupWizardContent(
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
                submitLabel = stringResource(R.string.groups_create)
            ),
            onBack = { onEvent(CreateGroupUiEvent.PreviousStep) },
            onNext = { onEvent(CreateGroupUiEvent.NextStep) },
            onSubmit = { onEvent(CreateGroupUiEvent.SubmitCreateGroup) },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}
