package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtraItemUiModel

@Composable
internal fun ExtrasBreakdownEntryRow(
    item: ExtraItemUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.parentTitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val subTextParts = mutableListOf<String>()
            if (item.dateText.isNotBlank()) {
                subTextParts.add(item.dateText)
            }
            if (!item.description.isNullOrBlank()) {
                subTextParts.add(item.description)
            }
            if (subTextParts.isNotEmpty()) {
                CaptionText(
                    text = subTextParts.joinToString(" · "),
                    maxLines = Int.MAX_VALUE
                )
            }
            if (item.scopeLabel.isNotBlank()) {
                CaptionText(
                    text = item.scopeLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
        Text(
            text = item.formattedAmount,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = MaterialTheme.spacing.Small)
        )
    }
}
