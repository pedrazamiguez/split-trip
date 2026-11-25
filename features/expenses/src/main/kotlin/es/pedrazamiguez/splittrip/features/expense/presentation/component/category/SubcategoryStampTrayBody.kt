package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubcategoryStampTrayBody(
    title: String,
    items: ImmutableList<SubcategoryStampItem>,
    onSubcategoryClicked: (String) -> Unit,
    showBadge: Boolean,
    retainedBadgeText: String,
    badgeStyle: PassportTagStyle,
    badgeModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SubcategoryTrayHeaderRow(
            title = title,
            showBadge = showBadge,
            retainedBadgeText = retainedBadgeText,
            badgeStyle = badgeStyle,
            badgeModifier = badgeModifier
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            items.forEach { item ->
                key(item.id) {
                    val debouncedClick = debounced { onSubcategoryClicked(item.id) }
                    PassportChip(
                        label = item.label,
                        selected = item.isSelected,
                        onClick = debouncedClick,
                        leadingIcon = filterChipLeadingIcon(
                            isSelected = item.isSelected,
                            unselectedIcon = item.icon
                        )
                    )
                }
            }
        }
    }
}
