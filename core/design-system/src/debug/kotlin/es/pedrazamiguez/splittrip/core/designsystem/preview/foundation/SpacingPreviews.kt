package es.pedrazamiguez.splittrip.core.designsystem.preview.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripSpacing
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun SpacingTokensPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            SpacingSwatch(label = "None", token = "0.dp", value = SplitTripSpacing.None)
            SpacingSwatch(label = "ExtraSmall", token = "4.dp", value = SplitTripSpacing.ExtraSmall)
            SpacingSwatch(label = "Small", token = "8.dp", value = SplitTripSpacing.Small)
            SpacingSwatch(label = "Medium", token = "12.dp", value = SplitTripSpacing.Medium)
            SpacingSwatch(label = "Default", token = "16.dp", value = SplitTripSpacing.Default)
            SpacingSwatch(label = "Large", token = "20.dp", value = SplitTripSpacing.Large)
            SpacingSwatch(label = "ExtraLarge", token = "24.dp", value = SplitTripSpacing.ExtraLarge)
            SpacingSwatch(label = "Section", token = "32.dp", value = SplitTripSpacing.Section)
            SpacingSwatch(label = "Screen", token = "48.dp", value = SplitTripSpacing.Screen)
        }
    }
}

/**
 * A single spacing swatch row: a filled bar whose width equals [value], flanked by token metadata.
 *
 * The bar is capped at the screen width so the largest tokens (Section, Screen) remain visible
 * without clipping the label column.
 */
@Composable
private fun SpacingSwatch(label: String, token: String, value: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        // Fixed-width label column so all bars start at the same x-offset.
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(96.dp)
        )

        // Coloured bar whose width mirrors the token value (capped to avoid overflow).
        val barWidth = value.coerceAtMost(200.dp)
        Box(
            modifier = Modifier
                .width(barWidth.coerceAtLeast(2.dp))
                .height(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraSmall
                )
        )

        // dp annotation for quick visual verification.
        Text(
            text = token,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
