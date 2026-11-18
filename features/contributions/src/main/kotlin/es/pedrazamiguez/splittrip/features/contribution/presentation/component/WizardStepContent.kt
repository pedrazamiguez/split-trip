package es.pedrazamiguez.splittrip.features.contribution.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardScrollableStepContainer
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.step.ContributionAmountStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.step.ContributionDateStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.step.ContributionReviewStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.step.ContributionScopeStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionStep
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

@Composable
internal fun WizardStepContent(
    uiState: AddContributionUiState,
    onEvent: (AddContributionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                .togetherWith(
                    slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                )
                .using(SizeTransform(clip = false))
        },
        label = "contributionWizardStep"
    ) { step ->
        WizardScrollableStepContainer {
            when (step) {
                AddContributionStep.AMOUNT -> ContributionAmountStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onSubmitKeyboard = { onEvent(AddContributionUiEvent.NextStep) }
                )
                AddContributionStep.SCOPE -> ContributionScopeStep(
                    uiState = uiState,
                    onEvent = onEvent
                )
                AddContributionStep.DATE -> ContributionDateStep(
                    uiState = uiState,
                    onEvent = onEvent
                )
                AddContributionStep.REVIEW -> ContributionReviewStep(
                    uiState = uiState
                )
            }
        }
    }
}
