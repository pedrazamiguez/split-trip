package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

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
