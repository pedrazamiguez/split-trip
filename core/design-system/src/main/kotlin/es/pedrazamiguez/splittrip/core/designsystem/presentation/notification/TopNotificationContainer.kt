package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard

/**
 * Shared Horizon container for top-overlay notification pills and in-app update banners.
 *
 * Centralizes safe-drawing window insets, outer horizontal/vertical margins, `shapes.medium`
 * geometry, light-mode ambient shadow (suppressed in dark mode by [FlatCard]), and optional
 * ghost borders for neutral containers.
 */
@Composable
fun TopNotificationContainer(
    style: TopPillStyle,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    ghostBorder: Boolean = style == TopPillStyle.NEUTRAL,
    content: @Composable () -> Unit
) {
    val colors = TopPillDefaults.colors(style)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
            .padding(
                horizontal = MaterialTheme.spacing.Default,
                vertical = MaterialTheme.spacing.Small
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        FlatCard(
            modifier = surfaceModifier.fillMaxWidth(),
            shape = TopPillDefaults.shape(),
            color = colors.containerColor,
            ghostBorder = ghostBorder,
            elevation = TopPillDefaults.Elevation,
            content = content
        )
    }
}
