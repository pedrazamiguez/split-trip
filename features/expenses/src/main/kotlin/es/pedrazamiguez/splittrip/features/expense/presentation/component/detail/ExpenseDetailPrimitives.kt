package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

internal val SECTION_ICON_SIZE = 16.dp

@Composable
internal fun SectionIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(SECTION_ICON_SIZE)
    )
}

/** Filled primary-container chip with icon + label. Used for the category in the hero. */
@Composable
internal fun CategoryChip(
    icon: ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        CaptionText(text = label, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Filled secondary-container chip with icon + label. Used for the payment method in the hero. */
@Composable
internal fun MethodChip(
    icon: ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        CaptionText(text = label, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** Filled tertiary-container chip with icon + label. Used for the payment status in the hero. */
@Composable
internal fun StatusBadgeChip(
    icon: ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        CaptionText(text = label, color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
}

/**
 * Outlined chip for the date in the hero. Uses a border tinted with [MaterialTheme.colorScheme.tertiary]
 * to visually relate to the status chip without competing with it.
 */
@Composable
internal fun DateChip(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp)
    ) {
        CaptionText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Neutral surface chip. Used for standalone status text without an icon. */
@Composable
internal fun StatusChip(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 4.dp)
    ) {
        CaptionText(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Small chip used in two contexts:
 * - [isPrimary] = true  → current user label in the split breakdown (primary colour)
 * - [isPrimary] = false → split-type label next to section header and subunit rows (filled surface)
 */
@Composable
internal fun SmallChip(text: String, isPrimary: Boolean) {
    val (backgroundColor, textColor) = if (isPrimary) {
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = MaterialTheme.spacing.Small, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
