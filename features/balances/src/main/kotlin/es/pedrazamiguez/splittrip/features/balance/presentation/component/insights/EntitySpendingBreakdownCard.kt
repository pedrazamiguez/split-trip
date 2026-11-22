package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntitySpendingInsightUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EntitySpendingBreakdownCard(
    hasSubunits: Boolean,
    selectedMode: EntityBreakdownMode,
    items: ImmutableList<EntitySpendingInsightUiModel>,
    onSelectMode: (EntityBreakdownMode) -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SectionHeadingText(text = stringResource(R.string.balances_insights_entity_title))

            if (hasSubunits) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                    val modes = listOf(
                        EntityBreakdownMode.BY_SUBUNIT to R.string.balances_insights_mode_subunits,
                        EntityBreakdownMode.BY_MEMBER to R.string.balances_insights_mode_members
                    )
                    modes.forEach { (mode, labelRes) ->
                        val isSelected = selectedMode == mode
                        val onChipClick = debounced { onSelectMode(mode) }
                        PassportChip(
                            label = stringResource(labelRes),
                            selected = isSelected,
                            onClick = onChipClick,
                            leadingIcon = if (isSelected) {
                                { Icon(imageVector = TablerIcons.Outline.Check, contentDescription = null) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
                items.forEach { item ->
                    EntitySpendingInsightRow(
                        item = item,
                        animationKey = items
                    )
                }
            }
        }
    }
}
