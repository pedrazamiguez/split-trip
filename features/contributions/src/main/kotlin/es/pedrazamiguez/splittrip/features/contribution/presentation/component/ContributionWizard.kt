package es.pedrazamiguez.splittrip.features.contribution.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

@Composable
internal fun ContributionWizard(
    uiState: AddContributionUiState,
    onEvent: (AddContributionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val amountLabel = stringResource(R.string.contribution_wizard_step_amount)
    val scopeLabel = stringResource(R.string.contribution_wizard_step_scope)
    val reviewLabel = stringResource(R.string.contribution_wizard_step_review)

    val stepLabelMap = remember(amountLabel, scopeLabel, reviewLabel) {
        mapOf(
            AddContributionStep.AMOUNT to amountLabel,
            AddContributionStep.SCOPE to scopeLabel,
            AddContributionStep.REVIEW to reviewLabel
        )
    }

    val orderedLabels = remember(uiState.steps, stepLabelMap) {
        uiState.steps.map { stepLabelMap[it] ?: "" }
    }

    val backLabel = stringResource(R.string.contribution_wizard_back)
    val nextLabel = stringResource(R.string.contribution_wizard_next)
    val submitLabel = stringResource(R.string.contribution_add_money_submit)

    val bottomPadding = LocalBottomPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WizardStepIndicator(
                stepLabels = orderedLabels,
                currentStepIndex = uiState.currentStepIndex,
                onStepClicked = { onEvent(AddContributionUiEvent.JumpToStep(it)) }
            )

            WizardStepContent(
                uiState = uiState,
                onEvent = onEvent,
                modifier = Modifier.weight(1f)
            )
        }

        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = uiState.canGoNext,
                isOnLastStep = uiState.isOnReviewStep,
                isCurrentStepValid = uiState.isCurrentStepValid,
                isLoading = uiState.isLoading,
                backLabel = backLabel,
                nextLabel = nextLabel,
                submitLabel = submitLabel
            ),
            onBack = { onEvent(AddContributionUiEvent.PreviousStep) },
            onNext = { onEvent(AddContributionUiEvent.NextStep) },
            onSubmit = { onEvent(AddContributionUiEvent.Submit) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}
