package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel
import kotlinx.collections.immutable.ImmutableList

private val SUBUNIT_ICON_SIZE = 18.dp
private val CHEVRON_SIZE = 18.dp
private val SUBUNIT_MEMBER_INDENT = 24.dp

@Composable
internal fun SplitBreakdownSection(
    splitTypeText: String,
    splits: ImmutableList<SplitDetailUiModel>,
    splitGroups: ImmutableList<SubunitSplitGroupUiModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Scale)
            LabelText(text = stringResource(R.string.expense_detail_section_split))
            SmallChip(text = splitTypeText, isPrimary = false)
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                splitGroups.forEach { group -> SubunitGroup(group) }
                splits.forEach { split -> SplitRow(split) }
            }
        }
    }
}

@Composable
private fun SubunitGroup(group: SubunitSplitGroupUiModel) {
    var expanded by remember(group.subunitId) { mutableStateOf(false) }
    // animateContentSize keeps the surrounding card from "snapping" when members are
    // revealed (Horizon Narrative §6.2 — micro-interactions should glide, not jump).
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.UsersGroup,
                    contentDescription = null,
                    modifier = Modifier.size(SUBUNIT_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = group.subunitLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CaptionText(
                        text = stringResource(
                            R.string.expense_detail_subunit_member_count,
                            group.memberCount
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                AmountText(text = group.formattedTotalAmount)
                Icon(
                    imageVector = if (expanded) {
                        TablerIcons.Outline.ChevronUp
                    } else {
                        TablerIcons.Outline.ChevronDown
                    },
                    contentDescription = null,
                    modifier = Modifier.size(CHEVRON_SIZE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .padding(top = MaterialTheme.spacing.Small, start = SUBUNIT_MEMBER_INDENT),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                group.members.forEach { member -> SplitRow(member) }
            }
        }
    }
}

@Composable
private fun SplitRow(split: SplitDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            SecondaryBodyText(
                text = if (split.isCurrentUser) {
                    stringResource(R.string.expense_detail_split_you_badge)
                } else {
                    split.displayName
                }
            )
            if (split.shareText != null) {
                CaptionText(
                    text = split.shareText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        AmountText(
            text = split.formattedAmount,
            color = if (split.isExcluded) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
