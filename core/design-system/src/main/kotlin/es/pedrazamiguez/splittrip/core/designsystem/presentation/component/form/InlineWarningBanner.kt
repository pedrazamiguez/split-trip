package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle

private val WARNING_ICON_SIZE = 16.dp

/**
 * A reusable inline warning banner for contextual, non-blocking messages.
 *
 * Uses [tertiaryContainer] / [onTertiaryContainer] colors (warm amber) to convey attention
 * without the severity of [FormErrorBanner]'s error red. This distinction matters: error styling
 * implies "you cannot proceed", whereas a warning is informational and non-blocking.
 *
 * Entry and exit are animated via [AnimatedVisibility] (fade + vertical expand/shrink) so the
 * surrounding layout shifts smoothly rather than jumping — consistent with the Horizon Narrative's
 * Optimistic Kineticism principle.
 *
 * Renders nothing (collapses with animation) when [warning] is null.
 *
 * @param warning The [UiText] to display, or null to hide the banner.
 * @param modifier Optional modifier applied to the [AnimatedVisibility] container.
 */
@Composable
fun InlineWarningBanner(
    warning: UiText?,
    modifier: Modifier = Modifier
) {
    // Cache the last non-null warning so the text remains visible during the exit animation.
    // Without this, warning becomes null before the fade/shrink completes, blanking the banner.
    var displayedWarning by remember { mutableStateOf(warning) }
    if (warning != null) displayedWarning = warning

    AnimatedVisibility(
        visible = warning != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.AlertTriangle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(WARNING_ICON_SIZE)
                )
                Text(
                    text = displayedWarning?.asString().orEmpty(),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
