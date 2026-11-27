package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonInfoContainer
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonInfoContainerDark
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonOnInfoContainer
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonOnInfoContainerDark
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonOnSuccessContainer
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonOnSuccessContainerDark
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonSuccessContainer
import es.pedrazamiguez.splittrip.core.designsystem.foundation.HorizonSuccessContainerDark
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AlertTriangle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
private const val SECONDARY_CONTENT_ALPHA = 0.85f
private const val SUCCESS_SECONDARY_CONTENT_ALPHA = 0.90f

@Immutable
data class TopPillColors(
    val containerColor: Color,
    val contentColor: Color,
    val secondaryContentColor: Color,
    val iconColor: Color,
    val actionContainerColor: Color,
    val actionContentColor: Color
)

object TopPillDefaults {
    val Elevation: Dp = 6.dp
    val LeadingIconSize: Dp = 20.dp
    val DismissIconSize: Dp = 16.dp
    val ActionMinHeight: Dp = 36.dp

    val EnterTransition: EnterTransition = slideInVertically(initialOffsetY = { -it }) + fadeIn()
    val ExitTransition: ExitTransition = slideOutVertically(targetOffsetY = { -it }) + fadeOut()

    @Composable
    fun shape(): Shape = MaterialTheme.shapes.medium

    @Composable
    fun colors(style: TopPillStyle): TopPillColors {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
        return when (style) {
            TopPillStyle.NEUTRAL -> neutralColors(isDark)
            TopPillStyle.INFO -> infoColors(isDark)
            TopPillStyle.SUCCESS -> successColors(isDark)
            TopPillStyle.WARNING -> TopPillColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                secondaryContentColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                    alpha = SECONDARY_CONTENT_ALPHA
                ),
                iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                actionContainerColor = MaterialTheme.colorScheme.tertiary,
                actionContentColor = MaterialTheme.colorScheme.onTertiary
            )

            TopPillStyle.ERROR -> TopPillColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                secondaryContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(
                    alpha = SECONDARY_CONTENT_ALPHA
                ),
                iconColor = MaterialTheme.colorScheme.onErrorContainer,
                actionContainerColor = MaterialTheme.colorScheme.error,
                actionContentColor = MaterialTheme.colorScheme.onError
            )
        }
    }

    fun icon(style: TopPillStyle): ImageVector? = when (style) {
        TopPillStyle.NEUTRAL -> null
        TopPillStyle.INFO -> TablerIcons.Outline.InfoCircle
        TopPillStyle.SUCCESS -> TablerIcons.Outline.CircleCheck
        TopPillStyle.WARNING,
        TopPillStyle.ERROR -> TablerIcons.Outline.AlertTriangle
    }
}

@Composable
private fun neutralColors(isDark: Boolean): TopPillColors = TopPillColors(
    containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    },
    contentColor = MaterialTheme.colorScheme.onSurface,
    secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    iconColor = MaterialTheme.colorScheme.primary,
    actionContainerColor = MaterialTheme.colorScheme.primary,
    actionContentColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun infoColors(isDark: Boolean): TopPillColors {
    val containerColor = if (isDark) HorizonInfoContainerDark else HorizonInfoContainer
    val contentColor = if (isDark) HorizonOnInfoContainerDark else HorizonOnInfoContainer
    return TopPillColors(
        containerColor = containerColor,
        contentColor = contentColor,
        secondaryContentColor = contentColor.copy(alpha = SECONDARY_CONTENT_ALPHA),
        iconColor = contentColor,
        actionContainerColor = MaterialTheme.colorScheme.primary,
        actionContentColor = MaterialTheme.colorScheme.onPrimary
    )
}

@Composable
private fun successColors(isDark: Boolean): TopPillColors {
    val containerColor = if (isDark) HorizonSuccessContainerDark else HorizonSuccessContainer
    val contentColor = if (isDark) HorizonOnSuccessContainerDark else HorizonOnSuccessContainer
    return TopPillColors(
        containerColor = containerColor,
        contentColor = contentColor,
        secondaryContentColor = contentColor.copy(alpha = SUCCESS_SECONDARY_CONTENT_ALPHA),
        iconColor = contentColor,
        actionContainerColor = contentColor,
        actionContentColor = containerColor
    )
}
