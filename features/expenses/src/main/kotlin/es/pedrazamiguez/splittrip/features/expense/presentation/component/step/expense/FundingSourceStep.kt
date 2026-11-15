package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.FundingSourceSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 3: Funding source selection.
 * Always shown — determines whether the expense is paid from "Group Pocket" or "Your Money".
 * Both options display persistent descriptions so users understand each choice before tapping.
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
    }
}
