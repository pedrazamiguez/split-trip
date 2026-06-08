package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AttachmentMetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isUri: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isUri) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (isUri) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
    }
}
