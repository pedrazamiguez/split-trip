package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText

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
