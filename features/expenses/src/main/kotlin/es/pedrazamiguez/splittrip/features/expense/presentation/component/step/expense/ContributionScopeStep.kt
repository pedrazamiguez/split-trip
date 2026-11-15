package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PayerTypeScopeCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PayerTypeScopeCardLabels
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Wizard step for selecting the contribution scope of an out-of-pocket expense.
 *
 * Only shown when the funding source is "Your Money" (USER). Lets the user choose
 * who gets credit for the paired contribution: Personal / Subunit / Group.
 * Auto-advances on GROUP or USER selections; SUBUNIT requires a sub-picker.
 */
@Composable
fun ContributionScopeStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onAutoAdvance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        PayerTypeScopeCard(
            labels = PayerTypeScopeCardLabels(
                title = stringResource(R.string.expense_contribution_scope_title),
                groupLabel = stringResource(R.string.expense_contribution_scope_for_group),
                personalLabel = stringResource(R.string.expense_contribution_scope_for_you),
                subunitLabelTemplate = stringResource(
                    R.string.expense_contribution_scope_for_subunit
                )
            ),
            selectedScope = uiState.contributionScope,
            selectedSubunitId = uiState.selectedContributionSubunitId,
            subunitOptions = uiState.contributionSubunitOptions,
            onScopeSelected = { scope, subunitId ->
                onEvent(AddExpenseUiEvent.ContributionScopeSelected(scope, subunitId))
                if (scope != PayerType.SUBUNIT) {
                    onAutoAdvance()
                }
            }
        )
    }
}
