package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText

private val LEADING_SLOT_SIZE = 40.dp
private val LEADING_ICON_SIZE = 22.dp

@Composable
internal fun SettingsSelectionLeadingBadge(
    leadingIcon: ImageVector?,
    leadingBadgeText: String?,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(LEADING_SLOT_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .background(containerColor)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(LEADING_ICON_SIZE)
            )
        } else if (!leadingBadgeText.isNullOrBlank()) {
            LabelText(
                text = leadingBadgeText,
                color = contentColor
            )
        }
    }
}
