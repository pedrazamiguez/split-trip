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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.model.MemberUiModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 4: Read-only summary of the subunit before saving.
 */
@Composable
fun SubunitReviewStep(
    uiState: CreateEditSubunitUiState,
    modifier: Modifier = Modifier
) {
    val none = stringResource(R.string.subunit_review_none)

    WizardStepLayout(modifier = modifier) {
        SectionCard(title = stringResource(R.string.subunit_review_title)) {
            ReviewCardContent(uiState = uiState, none = none)
        }

        FormErrorBanner(error = uiState.nameError)
        FormErrorBanner(error = uiState.membersError)
        FormErrorBanner(error = uiState.sharesError)
    }
}

@Composable
private fun ReviewCardContent(uiState: CreateEditSubunitUiState, none: String) {
    val memberMap = remember(uiState.availableMembers) {
        uiState.availableMembers.associateBy { it.userId }
    }

    ReviewRow(
        label = stringResource(R.string.subunit_review_name),
        value = uiState.name.ifBlank { none }
    )

    ReviewRow(
        label = stringResource(R.string.subunit_review_members),
        value = uiState.selectedMemberIds
            .mapNotNull { memberMap[it] }
            .joinToString { it.displayName }
            .ifBlank { none }
    )

    ReviewSharesList(uiState = uiState, memberMap = memberMap, none = none)
}

@Composable
private fun ReviewSharesList(
    uiState: CreateEditSubunitUiState,
    memberMap: Map<String, MemberUiModel>,
    none: String
) {
    val sharesLabel = stringResource(R.string.subunit_review_shares)
    val selectedMembers = uiState.selectedMemberIds.mapNotNull { memberMap[it] }

    if (selectedMembers.isEmpty()) {
        ReviewRow(label = sharesLabel, value = none)
    } else {
        Text(
            text = sharesLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        selectedMembers.forEach { member ->
            val shareText = uiState.memberShares[member.userId]
            val displayValue = if (shareText.isNullOrBlank()) none else "$shareText%"
            ReviewRow(label = member.displayName, value = displayValue)
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
