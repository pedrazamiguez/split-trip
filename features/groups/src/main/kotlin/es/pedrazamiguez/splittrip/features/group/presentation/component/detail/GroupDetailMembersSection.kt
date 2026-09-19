package es.pedrazamiguez.splittrip.features.group.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupMemberUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun GroupDetailMembersSection(
    members: ImmutableList<GroupMemberUiModel>,
    modifier: Modifier = Modifier
) {
    SectionCard(
        title = stringResource(R.string.group_detail_section_members),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            members.forEach { member ->
                GroupMemberItem(member = member)
            }
        }
    }
}
