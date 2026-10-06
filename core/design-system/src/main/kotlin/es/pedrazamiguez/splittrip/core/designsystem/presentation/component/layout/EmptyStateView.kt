package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Inbox

private const val OPTICAL_TOP_WEIGHT = 2f
private const val OPTICAL_BOTTOM_WEIGHT = 3f

/**
 * A modern empty state view following Material 3 Expressive principles.
 * Features large icon, bold headline, and subtle body text with optical vertical centering.
 */
@Suppress("CognitiveComplexMethod")
@Composable
fun EmptyStateView(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector = TablerIcons.Outline.Inbox,
    action: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Section),
        contentAlignment = Alignment.Center
    ) {
        val isHeightBounded = maxHeight != Dp.Infinity
        Column(
            modifier = if (isHeightBounded) Modifier.fillMaxSize() else Modifier.wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (isHeightBounded) Arrangement.Top else Arrangement.Center
        ) {
            if (isHeightBounded) {
                Spacer(modifier = Modifier.weight(OPTICAL_TOP_WEIGHT))
            }

            EmptyStateContent(
                title = title,
                description = description,
                icon = icon,
                action = action
            )

            if (isHeightBounded) {
                Spacer(modifier = Modifier.weight(OPTICAL_BOTTOM_WEIGHT))
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    title: String,
    description: String?,
    icon: ImageVector,
    action: (@Composable () -> Unit)?
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    if (description != null) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    if (action != null) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
        action()
    }
}
