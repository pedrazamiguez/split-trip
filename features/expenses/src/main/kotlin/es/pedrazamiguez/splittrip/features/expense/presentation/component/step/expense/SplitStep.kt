package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.InlineWarningBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.SplitSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 5: Split type + per-member allocation + sub-unit mode.
 * Only shown when the group has more than one member.
 */
@Composable
fun SplitStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        // Non-blocking contextual warning: the selected cash pool is personal (USER/SUBUNIT-scoped)
        // but the split currently includes members outside its natural scope.
        InlineWarningBanner(warning = uiState.personalCashSplitWarning)
        SplitSection(uiState = uiState, onEvent = onEvent)
    }
}
