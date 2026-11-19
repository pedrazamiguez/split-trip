package es.pedrazamiguez.splittrip.features.subunit.presentation.component

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
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitMembersStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitNameStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitReviewStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit.SubunitSharesStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitStep
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

@Composable
internal fun SubunitWizardStepContent(
    uiState: CreateEditSubunitUiState,
    onEvent: (CreateEditSubunitUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { it * direction } + fadeIn())
                .togetherWith(slideOutHorizontally { -it * direction } + fadeOut())
                .using(SizeTransform(clip = false))
        },
        label = "subunitWizardStep"
    ) { step ->
        WizardScrollableStepContainer {
            val nextStep = { onEvent(CreateEditSubunitUiEvent.NextStep) }
            when (step) {
                CreateEditSubunitStep.NAME -> SubunitNameStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CreateEditSubunitStep.MEMBERS -> SubunitMembersStep(uiState = uiState, onEvent = onEvent)
                CreateEditSubunitStep.SHARES -> SubunitSharesStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                CreateEditSubunitStep.REVIEW -> SubunitReviewStep(uiState = uiState)
            }
        }
    }
}
