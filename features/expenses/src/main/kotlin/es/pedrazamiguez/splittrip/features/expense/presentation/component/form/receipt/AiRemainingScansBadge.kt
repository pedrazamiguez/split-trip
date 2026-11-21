package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle

/**
 * Compact pill badge showing remaining monthly free AI receipt scans or quota exhaustion.
 */
@Composable
fun AiRemainingScansBadge(
    label: String,
    isExhausted: Boolean,
    modifier: Modifier = Modifier
) {
    val tagColors = PassportTagDefaults.colors(
        if (isExhausted) PassportTagStyle.ERROR else PassportTagStyle.INFO
    )
    val containerColor = tagColors.containerColor
    val contentColor = tagColors.contentColor
    val icon = if (isExhausted) {
        TablerIcons.Outline.Lock
    } else {
        TablerIcons.Outline.PhotoAi
    }

    Row(
        modifier = modifier
            .background(color = containerColor, shape = CircleShape)
            .padding(
                horizontal = MaterialTheme.spacing.Small,
                vertical = MaterialTheme.spacing.ExtraSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
