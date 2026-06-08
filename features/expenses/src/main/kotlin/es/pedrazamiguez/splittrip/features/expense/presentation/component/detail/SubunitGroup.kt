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
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel

private val SUBUNIT_ICON_SIZE = 18.dp
private val CHEVRON_SIZE = 18.dp
private val SUBUNIT_MEMBER_INDENT = 24.dp

@Suppress("LongMethod")
@Composable
internal fun SubunitGroup(group: SubunitSplitGroupUiModel) {
    var expanded by remember(group.subunitId) { mutableStateOf(false) }
    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = { expanded = !expanded },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                        text = group.subunitLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CaptionText(
                            text = stringResource(R.string.expense_detail_subunit_member_count, group.memberCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SmallChip(text = group.splitTypeText, isPrimary = false)
                    }
                }
            }
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
