package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

/** Shared icon size used across all button tiers for visual consistency. */
internal val BUTTON_ICON_SIZE = 18.dp

/**
 * Shared label + icon row used by [GradientButton], [SecondaryButton], and
 * [DestructiveButton].
 *
 * Renders an optional [leadingIcon], a bold [text] label, and an optional
 * [trailingIcon] in a horizontally-centered [Row]. Icons are sized to
 * [BUTTON_ICON_SIZE] for cross-button consistency.
 */
@Composable
internal fun ButtonContentRow(
    text: String,
    contentColor: Color,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(BUTTON_ICON_SIZE)
            )
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = if (leadingIcon != null) MaterialTheme.spacing.Small else 0.dp,
                end = if (trailingIcon != null) MaterialTheme.spacing.Small else 0.dp
            )
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(BUTTON_ICON_SIZE)
            )
        }
    }
}
