package es.pedrazamiguez.splittrip.features.contribution.presentation.component.step

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.MemberPickerCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.MemberPickerCardLabels
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PayerTypeScopeCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.PayerTypeScopeCardLabels
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

/**
 * Step 2: Scope selector — who the contribution is for.
 *
 * Lets the user choose:
 *   • Which group member is contributing (member picker — impersonation)
 *   • The whole group (GROUP)
 *   • A specific subunit (SUBUNIT)
 *   • Themselves (USER)
 */
@Composable
fun ContributionScopeStep(
    uiState: AddContributionUiState,
    onEvent: (AddContributionUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedMember = uiState.groupMembers.firstOrNull { it.userId == uiState.selectedMemberId }
    val personalLabel = if (selectedMember == null || selectedMember.isCurrentUser) {
        stringResource(R.string.contribution_add_money_for_me)
    } else {
        stringResource(R.string.contribution_add_money_for_member, selectedMember.displayName)
    }

    WizardStepLayout(modifier = modifier) {
        MemberPickerCard(
            labels = MemberPickerCardLabels(
                title = stringResource(R.string.contribution_member_picker_title),
                currentUserLabel = stringResource(R.string.contribution_member_picker_you_label)
            ),
            members = uiState.groupMembers,
            selectedMemberId = uiState.selectedMemberId,
            onMemberSelected = { userId ->
                onEvent(AddContributionUiEvent.MemberSelected(userId))
            }
        )

        PayerTypeScopeCard(
            labels = PayerTypeScopeCardLabels(
                title = stringResource(R.string.contribution_add_money_contributing_for),
                groupLabel = stringResource(R.string.contribution_add_money_for_group),
                personalLabel = personalLabel,
                subunitLabelTemplate = stringResource(R.string.contribution_add_money_for_subunit)
            ),
            selectedScope = uiState.contributionScope,
            selectedSubunitId = uiState.selectedSubunitId,
            subunitOptions = uiState.subunitOptions,
            onScopeSelected = { scope, subunitId ->
                onEvent(AddContributionUiEvent.ContributionScopeSelected(scope, subunitId))
            }
        )
    }
}
