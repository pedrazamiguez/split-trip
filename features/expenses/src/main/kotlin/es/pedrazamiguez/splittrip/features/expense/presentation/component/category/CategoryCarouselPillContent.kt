package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText

private val TRAILING_CHECK_ICON_SIZE = 14.dp

@Composable
internal fun CategoryCarouselPillContent(
    item: CategoryCarouselItem,
    contentColor: Color,
    horizontalPadding: Dp,
    iconSize: Dp,
    showCountBadge: Boolean,
    showTrailingCheck: Boolean,
    retainedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = horizontalPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            item.icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(iconSize)) }
            LabelText(text = item.label, color = contentColor, maxLines = 1)
        }
        AnimatedVisibility(
            visible = showCountBadge,
            enter = CategorySelectorTransitions.badgeEnterTransition(),
            exit = CategorySelectorTransitions.badgeExitTransition()
        ) {
            CategoryCarouselCountBadge(
                count = retainedCount,
                isFocused = item.isFocused,
                modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
            )
        }
        AnimatedVisibility(
            visible = showTrailingCheck,
            enter = CategorySelectorTransitions.badgeEnterTransition(),
            exit = CategorySelectorTransitions.badgeExitTransition()
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Check,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = MaterialTheme.spacing.Small)
                    .size(TRAILING_CHECK_ICON_SIZE)
            )
        }
    }
}
