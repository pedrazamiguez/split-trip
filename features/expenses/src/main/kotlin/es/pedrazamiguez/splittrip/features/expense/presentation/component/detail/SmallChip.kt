package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

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
