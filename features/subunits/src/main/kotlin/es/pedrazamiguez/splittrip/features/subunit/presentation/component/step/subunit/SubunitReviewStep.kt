package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 4: Read-only summary of the subunit before saving.
 */
@Suppress("LongMethod")
@Composable
fun SubunitReviewStep(
    uiState: CreateEditSubunitUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.subunit_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.subunit_review_title)) {
            val memberMap = remember(uiState.availableMembers) {
                uiState.availableMembers.associateBy { it.userId }
            }

            // Review Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SecondaryBodyText(text = stringResource(R.string.subunit_review_name), maxLines = Int.MAX_VALUE)
                Text(
                    text = uiState.name.ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // Review Members Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SecondaryBodyText(text = stringResource(R.string.subunit_review_members), maxLines = Int.MAX_VALUE)
                Text(
                    text = uiState.selectedMemberIds
                        .mapNotNull { memberMap[it] }
                        .joinToString { it.displayName }
                        .ifBlank { none },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            // Review Shares List
            val sharesLabel = stringResource(R.string.subunit_review_shares)
            val selectedMembers = uiState.selectedMemberIds.mapNotNull { memberMap[it] }

            if (selectedMembers.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SecondaryBodyText(text = sharesLabel, maxLines = Int.MAX_VALUE)
                    Text(
                        text = none,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                SecondaryBodyText(text = sharesLabel, maxLines = Int.MAX_VALUE)
                selectedMembers.forEach { member ->
                    val shareText = uiState.memberShares[member.userId]
                    val displayValue = if (shareText.isNullOrBlank()) none else "$shareText%"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SecondaryBodyText(text = member.displayName, maxLines = Int.MAX_VALUE)
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        FormErrorBanner(error = uiState.nameError)
        FormErrorBanner(error = uiState.membersError)
        FormErrorBanner(error = uiState.sharesError)
    }
}
