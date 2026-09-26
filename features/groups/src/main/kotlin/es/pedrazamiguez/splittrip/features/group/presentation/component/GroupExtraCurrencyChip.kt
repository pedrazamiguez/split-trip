package es.pedrazamiguez.splittrip.features.group.presentation.component

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

private val EXTRA_CURRENCY_HORIZONTAL_PADDING = 8.dp
private val EXTRA_CURRENCY_VERTICAL_PADDING = 3.dp

@Composable
internal fun GroupExtraCurrencyChip(
    currency: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Text(
            text = currency,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = EXTRA_CURRENCY_HORIZONTAL_PADDING,
                vertical = EXTRA_CURRENCY_VERTICAL_PADDING
            )
        )
    }
}
