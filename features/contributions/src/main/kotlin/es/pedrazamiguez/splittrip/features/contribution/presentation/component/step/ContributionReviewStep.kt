package es.pedrazamiguez.splittrip.features.contribution.presentation.component.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
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
            ReviewRow(
                label = stringResource(R.string.contribution_review_member),
                value = uiState.selectedMemberDisplayName.ifBlank { none }
            )

            ReviewRow(
                label = stringResource(R.string.contribution_review_amount),
                value = uiState.formattedAmountWithCurrency.ifBlank {
                    uiState.amountInput.ifBlank { none }
                }
            )

            ReviewRow(
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
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
