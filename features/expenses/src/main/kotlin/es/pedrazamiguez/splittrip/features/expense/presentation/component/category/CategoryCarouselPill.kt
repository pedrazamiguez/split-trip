package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText

private const val COLOR_ANIMATION_DURATION_MS = 200
private val PILL_COLOR_ANIMATION_SPEC = tween<Color>(COLOR_ANIMATION_DURATION_MS)
private val PILL_HEIGHT_STANDARD = 48.dp
private val PILL_HEIGHT_NARROW = 44.dp
private val PILL_ICON_SIZE_STANDARD = 20.dp
private val PILL_ICON_SIZE_NARROW = 18.dp
private val TRAILING_CHECK_ICON_SIZE = 14.dp

@Composable
internal fun CategoryCarouselPill(
    item: CategoryCarouselItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionRole: Role = Role.RadioButton
) {
    val isNarrow = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val pillHeight = if (isNarrow) PILL_HEIGHT_NARROW else PILL_HEIGHT_STANDARD
    val horizontalPadding = if (isNarrow) MaterialTheme.spacing.Medium else MaterialTheme.spacing.Default
    val iconSize = if (isNarrow) PILL_ICON_SIZE_NARROW else PILL_ICON_SIZE_STANDARD
    val colorScheme = MaterialTheme.colorScheme
    val infoColors = PassportTagDefaults.colors(PassportTagStyle.INFO)

    val containerColor by animateColorAsState(
        targetValue = resolveContainerColor(item, colorScheme, infoColors),
        animationSpec = PILL_COLOR_ANIMATION_SPEC,
        label = "CategoryCarouselPillContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = resolveContentColor(item, colorScheme, infoColors),
        animationSpec = PILL_COLOR_ANIMATION_SPEC,
        label = "CategoryCarouselPillContentColor"
    )

    Surface(
        modifier = modifier
            .height(pillHeight)
            .clip(CircleShape)
            .semantics { selected = item.isSelected }
            .debouncedClickable(enableSpringPress = true, role = selectionRole, onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            modifier = Modifier.padding(horizontal = horizontalPadding)
        ) {
            item.icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(iconSize)) }
            LabelText(text = item.label, color = contentColor, maxLines = 1)
            if (item.activeSubcategoryCount > 0) {
                CategoryCarouselCountBadge(count = item.activeSubcategoryCount, isFocused = item.isFocused)
            } else if (item.isSelected && !item.isFocused) {
                Icon(
                    imageVector = TablerIcons.Outline.Check,
                    contentDescription = null,
                    modifier = Modifier.size(TRAILING_CHECK_ICON_SIZE)
                )
            }
        }
    }
}

private fun resolveContainerColor(
    item: CategoryCarouselItem,
    colorScheme: ColorScheme,
    infoColors: PassportTagColors
): Color = when {
    item.isSelected && item.isFocused -> colorScheme.primary
    item.isSelected -> infoColors.containerColor
    else -> colorScheme.surfaceContainerHigh
}

private fun resolveContentColor(
    item: CategoryCarouselItem,
    colorScheme: ColorScheme,
    infoColors: PassportTagColors
): Color = when {
    item.isSelected && item.isFocused -> colorScheme.onPrimary
    item.isSelected -> infoColors.contentColor
    else -> colorScheme.onSurfaceVariant
}
