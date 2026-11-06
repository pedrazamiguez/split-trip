package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * **Passport Tag** — Standardized non-interactive metadata tag / status badge.
 *
 * Adheres strictly to Horizon Narrative design principles:
 * - **Full-pill shape:** [CircleShape].
 * - **Fixed height contract:** [PassportTagDefaults.Height] (28.dp) preventing height jitter.
 * - **No border:** Background tone defines boundaries (Horizon No-Line rule).
 * - **Standardized icon & text alignment:** 16.dp icon size, 4.dp spacing, labelSmall typography.
 */
@Composable
fun PassportTag(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PassportTagStyle = PassportTagStyle.NEUTRAL,
    iconContentDescription: String? = null
) {
    val colors = PassportTagDefaults.colors(style)

    Surface(
        modifier = modifier.height(PassportTagDefaults.Height),
        shape = CircleShape,
        color = colors.containerColor,
        contentColor = colors.contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PassportTagDefaults.HorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PassportTagDefaults.IconSpacing)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier.size(PassportTagDefaults.IconSize),
                    tint = colors.contentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = colors.contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
