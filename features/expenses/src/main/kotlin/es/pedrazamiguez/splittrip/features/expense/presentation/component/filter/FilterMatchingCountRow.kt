package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlignJustified
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.features.expense.R

private val MATCHING_COUNT_ICON_SIZE = 18.dp

@Composable
internal fun FilterMatchingCountRow(
    matchingExpensesCount: Int,
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(
            top = MaterialTheme.spacing.Small,
            bottom = MaterialTheme.spacing.Large
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Icon(
            imageVector = if (hasActiveFilters) {
                TablerIcons.Outline.AlignJustified
            } else {
                TablerIcons.Outline.Receipt
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MATCHING_COUNT_ICON_SIZE)
        )
        Text(
            text = pluralStringResource(
                R.plurals.expenses_filter_matching_count,
                matchingExpensesCount,
                matchingExpensesCount
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
