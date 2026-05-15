package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SplitBreakdownSection(
    splitTypeText: String,
    splits: ImmutableList<SplitDetailUiModel>
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
                splits.forEach { split ->
                    SplitRow(split = split)
                }
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (split.shareText != null) {
                CaptionText(
                    text = split.shareText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BodyText(
            text = split.formattedAmount,
            color = if (split.isExcluded) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
