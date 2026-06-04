package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

@Composable
internal fun ExpenseWizardBody(
    uiState: AddExpenseUiState,
    orderedLabels: List<String>,
    skipToReviewLabel: String,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WizardStepIndicator(
            stepLabels = orderedLabels,
            currentStepIndex = uiState.currentStepIndex,
            optionalStepIndices = uiState.optionalStepIndices,
            skipToReviewLabel = if (uiState.canSkipToReview) skipToReviewLabel else null,
            onSkipToReview = if (uiState.canSkipToReview) {
                { onEvent(AddExpenseUiEvent.JumpToReview) }
            } else {
                null
            },
            allowForwardJumps = uiState.isEditMode,
            onStepClicked = { onEvent(AddExpenseUiEvent.JumpToStep(it)) }
        )

        AnimatedVisibility(
            visible = uiState.autoFillBanner != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            uiState.autoFillBanner?.let { banner ->
                AutoFillBanner(
                    banner = banner,
                    onDismiss = { onEvent(AddExpenseUiEvent.DismissAutoFillBanner) }
                )
            }
        }

        WizardStepContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.weight(1f)
        )
    }
}
