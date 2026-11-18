package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

private const val COLOR_ANIMATION_DURATION_MS = 200
private val TILE_MIN_HEIGHT = 76.dp
private val TILE_ICON_SIZE = 24.dp

/**
 * Fixed-size category or subcategory grid tile displaying an icon above a centered label.
 */
@Composable
internal fun CategoryGridTile(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val horizontalPadding =
        if (isNarrowScreen) MaterialTheme.spacing.ExtraSmall else MaterialTheme.spacing.Small

    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.secondaryFixedDim else colorScheme.surfaceContainerLow,
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "CategoryGridTileContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onSecondaryFixed else colorScheme.onSurfaceVariant,
        animationSpec = tween(COLOR_ANIMATION_DURATION_MS),
        label = "CategoryGridTileContentColor"
    )

    FlatCard(
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        modifier = modifier
            .heightIn(min = TILE_MIN_HEIGHT)
            .clip(MaterialTheme.shapes.medium)
            .semantics { this.selected = selected }
            .debouncedClickable(role = Role.Checkbox, enableSpringPress = true, onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = MaterialTheme.spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small, Alignment.CenterVertically)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(TILE_ICON_SIZE)
                )
            }
            CaptionText(text = label, color = contentColor, maxLines = 2, textAlign = TextAlign.Center)
        }
    }
}
