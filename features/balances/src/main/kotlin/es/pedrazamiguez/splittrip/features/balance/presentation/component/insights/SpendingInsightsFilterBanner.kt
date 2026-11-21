package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Filter
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.features.balance.R

private val FILTER_ICON_CONTAINER_SIZE = 36.dp
private val FILTER_ICON_SIZE = 18.dp

@Composable
internal fun SpendingInsightsFilterBanner(
    activeFilterCount: Int,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onDebouncedClear = debounced(onClick = onClearFilters)

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.Small,
                    end = MaterialTheme.spacing.Small,
                    bottom = MaterialTheme.spacing.Small
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(FILTER_ICON_CONTAINER_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Filter,
                    contentDescription = null,
                    modifier = Modifier.size(FILTER_ICON_SIZE),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                CardTitleText(text = stringResource(R.string.balances_insights_filtered_banner_title))
                if (activeFilterCount > 0) {
                    PassportTag(
                        text = pluralStringResource(
                            R.plurals.balances_insights_filter_count,
                            activeFilterCount,
                            activeFilterCount
                        ),
                        style = PassportTagStyle.INFO
                    )
                }
            }

            TextButton(onClick = onDebouncedClear) {
                Text(text = stringResource(R.string.balances_insights_filtered_banner_clear))
            }
        }
    }
}
