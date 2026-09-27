package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Scale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubunitSplitGroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SplitBreakdownSection(
    splitTypeText: String,
    splitBreakdownItems: ImmutableList<SplitBreakdownItemUiModel>,
    splits: ImmutableList<SplitDetailUiModel> = persistentListOf(),
    splitGroups: ImmutableList<SubunitSplitGroupUiModel> = persistentListOf()
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
                if (splitBreakdownItems.isNotEmpty()) {
                    splitBreakdownItems.forEach { item ->
                        when (item) {
                            is SplitBreakdownItemUiModel.Solo -> SplitRow(item.split)
                            is SplitBreakdownItemUiModel.Subunit -> SubunitGroup(item.group)
                        }
                    }
                } else {
                    splitGroups.forEach { group -> SubunitGroup(group) }
                    splits.forEach { split -> SplitRow(split) }
                }
            }
        }
    }
}
