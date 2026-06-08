package es.pedrazamiguez.splittrip.features.subunit.presentation.component.step.subunit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Step 2: Select members from the group.
 */
@Suppress("CognitiveComplexMethod")
@Composable
fun SubunitMembersStep(
    uiState: CreateEditSubunitUiState,
    onEvent: (CreateEditSubunitUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        FormErrorBanner(error = uiState.membersError)

        Column {
            uiState.availableMembers.forEach { member ->
                val isSelected = member.userId in uiState.selectedMemberIds
                val isEnabled = !member.isAssigned

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.Small)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (isEnabled) onEvent(CreateEditSubunitUiEvent.ToggleMember(member.userId))
                        },
                        enabled = isEnabled
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.ExtraSmall))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (member.isAssigned) {
                            SecondaryBodyText(
                                text = stringResource(
                                    R.string.subunit_member_assigned_hint,
                                    member.assignedSubunitName
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = Int.MAX_VALUE
                            )
                        }
                    }
                }
            }
        }
    }
}
