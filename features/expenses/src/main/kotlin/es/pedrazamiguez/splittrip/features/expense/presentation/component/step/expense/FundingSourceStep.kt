package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.FundingSourceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 3: Funding source selection.
 * Always shown — determines whether the expense is paid from the group pocket or personal money.
 * Shows a contextual hint when "My Money" is selected.
 * Auto-advances to the next step after a selection is made.
 */
@Composable
fun FundingSourceStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onAutoAdvance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        FundingSourceSection(
            fundingSources = uiState.fundingSources,
            selectedFundingSource = uiState.selectedFundingSource,
            onFundingSourceSelected = { sourceId ->
                onEvent(AddExpenseUiEvent.FundingSourceSelected(sourceId))
                onAutoAdvance()
            }
        )

        AnimatedVisibility(visible = uiState.fundingSourceHint != null) {
            uiState.fundingSourceHint?.let { hint ->
                SecondaryBodyText(
                    text = hint.asString(),
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.ExtraSmall)
                )
            }
        }
    }
}
