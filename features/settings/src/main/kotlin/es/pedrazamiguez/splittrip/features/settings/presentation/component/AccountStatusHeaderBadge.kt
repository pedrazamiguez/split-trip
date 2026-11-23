package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield

private val HEADER_BADGE_SIZE = 44.dp
private val HEADER_ICON_SIZE = 22.dp
private const val GUEST_BADGE_ALPHA = 0.5f
private const val VERIFIED_BADGE_ALPHA = 0.4f

@Composable
internal fun AccountStatusHeaderBadge(
    isAnonymous: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isAnonymous) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = GUEST_BADGE_ALPHA)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = VERIFIED_BADGE_ALPHA)
    }
    val icon = if (isAnonymous) TablerIcons.Outline.AlertTriangle else TablerIcons.Outline.Shield
    val tint = if (isAnonymous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(HEADER_BADGE_SIZE)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(HEADER_ICON_SIZE)
        )
    }
}
