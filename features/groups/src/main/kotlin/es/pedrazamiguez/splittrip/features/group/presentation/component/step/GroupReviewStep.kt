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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

private const val LABEL_WEIGHT = 1f
private const val VALUE_WEIGHT = 1.5f

/**
 * Step 4: Read-only summary of all entered data — final confirmation before creation.
 */
@Suppress("LongMethod")
@Composable
fun GroupReviewStep(
    uiState: CreateGroupUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.group_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.group_review_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.group_review_name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_WEIGHT)
                )
                Text(
                    text = uiState.groupName.ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(VALUE_WEIGHT)
                )
            }
            if (uiState.groupDescription.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.group_review_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = uiState.groupDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BodyText(
                    text = stringResource(R.string.group_review_currency),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(LABEL_WEIGHT)
                )
                Text(
                    text = uiState.selectedCurrency?.displayText ?: none,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(VALUE_WEIGHT)
                )
            }
            if (uiState.extraCurrencies.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.group_review_extra_currencies),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = uiState.extraCurrencies.joinToString { it.code },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT)
                    )
                }
            }
            if (uiState.selectedMembers.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BodyText(
                        text = stringResource(R.string.group_review_members),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(LABEL_WEIGHT)
                    )
                    Text(
                        text = uiState.selectedMembers.joinToString { it.displayName ?: it.email },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(VALUE_WEIGHT)
                    )
                }
            }
        }
    }
}
