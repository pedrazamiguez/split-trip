package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

private const val COLOR_ANIMATION_DURATION_MS = 200
private val PILL_COLOR_ANIMATION_SPEC = tween<Color>(COLOR_ANIMATION_DURATION_MS)
private val PILL_HEIGHT_STANDARD = 42.dp
private val PILL_HEIGHT_NARROW = 38.dp
private val PILL_HORIZONTAL_PADDING_NARROW = 10.dp
private val PILL_ICON_SIZE_STANDARD = 18.dp
private val PILL_ICON_SIZE_NARROW = 16.dp

@Composable
internal fun CategoryCarouselPill(
    item: CategoryCarouselItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionRole: Role = Role.RadioButton
) {
    val isNarrow = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val pillHeight = if (isNarrow) PILL_HEIGHT_NARROW else PILL_HEIGHT_STANDARD
    val horizontalPadding = if (isNarrow) PILL_HORIZONTAL_PADDING_NARROW else MaterialTheme.spacing.Medium
    val iconSize = if (isNarrow) PILL_ICON_SIZE_NARROW else PILL_ICON_SIZE_STANDARD
    val colorScheme = MaterialTheme.colorScheme
    val infoColors = PassportTagDefaults.colors(PassportTagStyle.INFO)

    val showCountBadge = item.activeSubcategoryCount > 0
    val showTrailingCheck = !showCountBadge && item.isSelected && !item.isFocused
    var retainedCount by remember { mutableIntStateOf(item.activeSubcategoryCount) }
    if (item.activeSubcategoryCount > 0) {
        retainedCount = item.activeSubcategoryCount
    }

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
        CategoryCarouselPillContent(
            item = item,
            contentColor = contentColor,
            horizontalPadding = horizontalPadding,
            iconSize = iconSize,
            showCountBadge = showCountBadge,
            showTrailingCheck = showTrailingCheck,
            retainedCount = retainedCount
        )
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
