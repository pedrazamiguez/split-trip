package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable member picker card for selecting a group member from a radio group.
 *
 * Renders a card with a title and a radio button for each available member.
 * The current user's name is visually distinguished with a localised suffix
 * (e.g. "Ana (You)").
 *
 * Designed for use in contribution and cash withdrawal flows where a user can
 * perform actions on behalf of another group member (impersonation).
 *
 * @param labels            Localised labels for the card title and current-user suffix.
 * @param members           Available group members to render as radio rows.
 * @param selectedMemberId  ID of the currently selected member, or `null` if none selected.
 * @param onMemberSelected  Callback emitting the chosen member's user ID.
 */
@Composable
fun MemberPickerCard(
    labels: MemberPickerCardLabels,
    members: ImmutableList<MemberOptionUiModel>,
    selectedMemberId: String?,
    onMemberSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.Large)) {
            Text(
                text = labels.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
            Column(modifier = Modifier.selectableGroup()) {
                members.forEach { member ->
                    val displayText = if (member.isCurrentUser) {
                        "${member.displayName} ${labels.currentUserSuffix}"
                    } else {
                        member.displayName
                    }
                    MemberRadioRow(
                        text = displayText,
                        selected = member.userId == selectedMemberId,
                        onClick = { onMemberSelected(member.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberRadioRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
        )
    }
}
