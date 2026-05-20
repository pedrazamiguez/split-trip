package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
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
    Column(
        modifier = Modifier.padding(top = MaterialTheme.spacing.Small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
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
        SubunitGroupHeader(
            group = group,
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )
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
private fun SubunitGroupHeader(
    group: SubunitSplitGroupUiModel,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggle,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubunitGroupHeaderInfo(
            label = group.subunitLabel,
            memberCount = group.memberCount,
            splitTypeText = group.splitTypeText
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                AmountText(text = group.formattedTotalAmount)
                if (group.formattedSourceTotalAmount != null) {
                    CaptionText(
                        text = group.formattedSourceTotalAmount,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (expanded) TablerIcons.Outline.ChevronUp else TablerIcons.Outline.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(CHEVRON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubunitGroupHeaderInfo(
    label: String,
    memberCount: Int,
    splitTypeText: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Sitemap,
            contentDescription = null,
            modifier = Modifier.size(SUBUNIT_ICON_SIZE),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CaptionText(
                    text = stringResource(R.string.expense_detail_subunit_member_count, memberCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SmallChip(text = splitTypeText, isPrimary = false)
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
            Text(
                text = if (split.isCurrentUser) {
                    stringResource(R.string.expense_detail_split_you_badge)
                } else {
                    split.displayName
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (split.shareText != null) {
                CaptionText(
                    text = split.shareText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Reserve trailing space to align with header's chevron icon (ExtraSmall spacing + 18dp icon)
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = MaterialTheme.spacing.ExtraSmall + CHEVRON_SIZE)
        ) {
            AmountText(
                text = split.formattedAmount,
                color = if (split.isExcluded) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (split.formattedSourceAmount != null) {
                CaptionText(
                    text = split.formattedSourceAmount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
