package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.settings.R

private val ACTION_PILL_HEIGHT = 36.dp
private const val DISABLED_CONTAINER_ALPHA = 0.08f
private const val DISABLED_CONTENT_ALPHA = 0.38f

@Composable
internal fun AccountProviderActionButton(
    isLinked: Boolean,
    isActionLoading: Boolean,
    canUnlink: Boolean,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionText = stringResource(
        if (isLinked) R.string.account_status_unlink_button else R.string.account_status_link_button
    )
    val actionEnabled = !isActionLoading && (!isLinked || canUnlink)
    val actionOnClick = if (isLinked) onUnlinkClick else onLinkClick
    val containerColor = when {
        !actionEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
        isLinked -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when {
        !actionEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
        isLinked -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(ACTION_PILL_HEIGHT)
            .clip(CircleShape)
            .background(containerColor)
            .debouncedClickable(
                enabled = actionEnabled,
                role = Role.Button,
                onClick = actionOnClick
            )
            .padding(horizontal = MaterialTheme.spacing.Default)
    ) {
        LabelText(
            text = actionText,
            color = contentColor
        )
    }
}
