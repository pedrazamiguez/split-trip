package es.pedrazamiguez.splittrip.features.group.presentation.component.step

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
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

/**
 * Step 4: Read-only summary of all entered data — final confirmation before creation.
 */
@Composable
fun GroupReviewStep(
    uiState: CreateGroupUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.group_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.group_review_title)) {
            ReviewRow(
                label = stringResource(R.string.group_review_name),
                value = uiState.groupName.ifBlank { none }
            )
            if (uiState.groupDescription.isNotBlank()) {
                ReviewRow(
                    label = stringResource(R.string.group_review_description),
                    value = uiState.groupDescription
                )
            }
            ReviewRow(
                label = stringResource(R.string.group_review_currency),
                value = uiState.selectedCurrency?.displayText ?: none
            )
            if (uiState.extraCurrencies.isNotEmpty()) {
                ReviewRow(
                    label = stringResource(R.string.group_review_extra_currencies),
                    value = uiState.extraCurrencies.joinToString { it.code }
                )
            }
            if (uiState.selectedMembers.isNotEmpty()) {
                ReviewRow(
                    label = stringResource(R.string.group_review_members),
                    value = uiState.selectedMembers.joinToString { it.displayName ?: it.email }
                )
            }
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(LABEL_WEIGHT)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(VALUE_WEIGHT)
        )
    }
}

private const val LABEL_WEIGHT = 1f
private const val VALUE_WEIGHT = 1.5f
