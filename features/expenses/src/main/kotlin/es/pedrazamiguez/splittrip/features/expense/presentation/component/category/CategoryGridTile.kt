package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.ClickableInteraction
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

private const val COLOR_ANIMATION_DURATION_MS = 200
private val TILE_COLOR_ANIMATION_SPEC = tween<Color>(COLOR_ANIMATION_DURATION_MS)
private val ICON_CONTAINER_SIZE_STANDARD = 56.dp
private val ICON_CONTAINER_SIZE_NARROW = 52.dp
private val TILE_ICON_SIZE = 24.dp

/**
 * Cardless category or subcategory grid item displaying a rounded icon surface above a centered label.
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
    val iconContainerSize = if (isNarrowScreen) ICON_CONTAINER_SIZE_NARROW else ICON_CONTAINER_SIZE_STANDARD
    val colorScheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        targetValue = if (selected) colorScheme.secondaryFixedDim else colorScheme.surfaceContainerHigh,
        animationSpec = TILE_COLOR_ANIMATION_SPEC,
        label = "CategoryGridTileContainerColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onSecondaryFixed else colorScheme.onSurfaceVariant,
        animationSpec = TILE_COLOR_ANIMATION_SPEC,
        label = "CategoryGridTileIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) colorScheme.onSurface else colorScheme.onSurfaceVariant,
        animationSpec = TILE_COLOR_ANIMATION_SPEC,
        label = "CategoryGridTileLabelColor"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val interaction = ClickableInteraction(interactionSource, indication = null, enableSpringPress = true)

    Column(
        modifier = modifier
            .semantics { this.selected = selected }
            .debouncedClickable(interaction = interaction, role = Role.Checkbox, onClick = onClick)
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        Surface(
            modifier = Modifier.size(iconContainerSize),
            shape = MaterialTheme.shapes.medium,
            color = containerColor,
            contentColor = iconColor
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(TILE_ICON_SIZE))
                }
            }
        }
        CaptionText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = labelColor,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}
