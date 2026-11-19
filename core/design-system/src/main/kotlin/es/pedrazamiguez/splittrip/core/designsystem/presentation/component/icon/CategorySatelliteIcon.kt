package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

private val DEFAULT_CONTAINER_SIZE = 44.dp
private const val ICON_RATIO = 0.5f
private const val SATELLITE_RATIO = 0.5f
private const val SATELLITE_ICON_RATIO = 0.27f
private val SATELLITE_OFFSET = 2.dp
private val SATELLITE_BORDER_WIDTH = 1.5.dp

/**
 * Renders a primary parent category icon within a [containerSize] container with an optional satellite
 * subcategory badge positioned at the bottom-end corner when a non-unspecified subcategory is selected.
 */
@Composable
fun CategorySatelliteIcon(
    category: ExpenseCategory,
    subcategory: ExpenseSubcategory,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    containerSize: Dp = DEFAULT_CONTAINER_SIZE
) {
    val iconSize = containerSize * ICON_RATIO
    val satelliteSize = containerSize * SATELLITE_RATIO
    val satelliteIconSize = containerSize * SATELLITE_ICON_RATIO

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(containerSize)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.toIconVector(),
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (subcategory != ExpenseSubcategory.UNSPECIFIED) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = SATELLITE_OFFSET, y = SATELLITE_OFFSET)
                    .size(satelliteSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        width = SATELLITE_BORDER_WIDTH,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subcategory.toIconVector(),
                    contentDescription = null,
                    modifier = Modifier.size(satelliteIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
