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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

/**
 * Step 3: Read-only summary of all entered data (final confirmation).
 * Always shown as the last wizard step.
 */
@Suppress("LongMethod")
@Composable
fun ContributionReviewStep(
    uiState: AddContributionUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.contribution_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.contribution_review_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.contribution_review_member),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.selectedMemberDisplayName.ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.contribution_review_amount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.formattedAmountWithCurrency.ifBlank {
                        uiState.amountInput.ifBlank { none }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.contribution_review_scope),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (uiState.contributionScope) {
                        PayerType.GROUP ->
                            stringResource(R.string.contribution_review_scope_group)
                        PayerType.USER ->
                            stringResource(R.string.contribution_review_scope_personal)
                        PayerType.SUBUNIT ->
                            uiState.subunitOptions
                                .find { it.id == uiState.selectedSubunitId }?.name ?: none
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
