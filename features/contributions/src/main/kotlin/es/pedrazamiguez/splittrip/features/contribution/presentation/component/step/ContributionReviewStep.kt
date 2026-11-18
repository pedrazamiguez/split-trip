package es.pedrazamiguez.splittrip.features.contribution.presentation.component.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

/**
 * Step 3: Read-only summary of all entered data (final confirmation).
 * Always shown as the last wizard step.
 */
@Composable
fun ContributionReviewStep(
    uiState: AddContributionUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.contribution_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.contribution_review_title)) {
            WizardReviewRow(
                label = stringResource(R.string.contribution_review_member),
                value = uiState.selectedMemberDisplayName.ifBlank { none }
            )

            WizardReviewRow(
                label = stringResource(R.string.contribution_review_amount),
                value = uiState.formattedAmountWithCurrency.ifBlank {
                    uiState.amountInput.ifBlank { none }
                }
            )

            WizardReviewRow(
                label = stringResource(R.string.contribution_review_scope),
                value = when (uiState.contributionScope) {
                    PayerType.GROUP ->
                        stringResource(R.string.contribution_review_scope_group)
                    PayerType.USER ->
                        stringResource(R.string.contribution_review_scope_personal)
                    PayerType.SUBUNIT ->
                        uiState.subunitOptions
                            .find { it.id == uiState.selectedSubunitId }?.name ?: none
                }
            )

            WizardReviewRow(
                label = stringResource(R.string.contribution_wizard_step_date),
                value = uiState.formattedContributionDate.ifBlank { none }
            )
        }
    }
}
