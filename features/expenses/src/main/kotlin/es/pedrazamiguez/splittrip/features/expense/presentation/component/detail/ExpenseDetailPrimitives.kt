package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

internal val SECTION_ICON_SIZE = 16.dp

private const val DETAIL_ROW_LABEL_WEIGHT = 0.4f
private const val DETAIL_ROW_VALUE_WEIGHT = 0.6f

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        SecondaryBodyText(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(DETAIL_ROW_LABEL_WEIGHT)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(DETAIL_ROW_VALUE_WEIGHT)
        )
    }
}

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

/** Neutral surface chip. Used for payment status. */
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
 * Small outlined chip used in two contexts:
 * - [isPrimary] = true  → current user label in the split breakdown (primary colour)
 * - [isPrimary] = false → split-type label next to section header (neutral colour)
 */
@Composable
internal fun SmallChip(text: String, isPrimary: Boolean) {
    val borderColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val textColor = if (isPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
