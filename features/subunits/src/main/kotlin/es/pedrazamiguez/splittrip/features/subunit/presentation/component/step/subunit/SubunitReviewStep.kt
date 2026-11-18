package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
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
            val memberMap = remember(uiState.availableMembers) {
                uiState.availableMembers.associateBy { it.userId }
            }

            WizardReviewRow(
                label = stringResource(R.string.subunit_review_name),
                value = uiState.name.ifBlank { none }
            )

            WizardReviewRow(
                label = stringResource(R.string.subunit_review_members),
                value = uiState.selectedMemberIds
                    .mapNotNull { memberMap[it] }
                    .joinToString { it.displayName }
                    .ifBlank { none }
            )

            val sharesLabel = stringResource(R.string.subunit_review_shares)
            val selectedMembers = uiState.selectedMemberIds.mapNotNull { memberMap[it] }

            if (selectedMembers.isEmpty()) {
                WizardReviewRow(
                    label = sharesLabel,
                    value = none
                )
            } else {
                SecondaryBodyText(text = sharesLabel, maxLines = Int.MAX_VALUE)
                selectedMembers.forEach { member ->
                    val shareText = uiState.memberShares[member.userId]
                    val displayValue = if (shareText.isNullOrBlank()) none else "$shareText%"
                    WizardReviewRow(
                        label = member.displayName,
                        value = displayValue
                    )
                }
            }
        }

        FormErrorBanner(error = uiState.nameError)
        FormErrorBanner(error = uiState.membersError)
        FormErrorBanner(error = uiState.sharesError)
    }
}
