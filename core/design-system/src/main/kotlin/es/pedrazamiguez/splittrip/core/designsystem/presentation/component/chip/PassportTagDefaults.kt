package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f

@Immutable
data class PassportTagColors(
    val containerColor: Color,
    val contentColor: Color
)

object PassportTagDefaults {
    val Height: Dp = 28.dp
    val IconSize: Dp = 16.dp
    val IconSpacing: Dp = 4.dp
    val HorizontalPadding: Dp = 8.dp

    @Composable
    fun colors(style: PassportTagStyle): PassportTagColors {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
        val (container, content) = when (style) {
            PassportTagStyle.PRIMARY ->
                MaterialTheme.colorScheme.primaryContainer to
                    MaterialTheme.colorScheme.onPrimaryContainer

            PassportTagStyle.INFO -> if (isDark) {
                HorizonInfoContainerDark to HorizonOnInfoContainerDark
            } else {
                HorizonInfoContainer to HorizonOnInfoContainer
            }

            PassportTagStyle.SUCCESS -> if (isDark) {
                HorizonSuccessContainerDark to HorizonOnSuccessContainerDark
            } else {
                HorizonSuccessContainer to HorizonOnSuccessContainer
            }

            PassportTagStyle.TERTIARY,
            PassportTagStyle.WARNING ->
                MaterialTheme.colorScheme.tertiaryContainer to
                    MaterialTheme.colorScheme.onTertiaryContainer

            PassportTagStyle.ERROR ->
                MaterialTheme.colorScheme.errorContainer to
                    MaterialTheme.colorScheme.onErrorContainer

            PassportTagStyle.NEUTRAL ->
                MaterialTheme.colorScheme.surfaceContainerHigh to
                    MaterialTheme.colorScheme.onSurfaceVariant
        }
        return PassportTagColors(
            containerColor = container,
            contentColor = content
        )
    }
}
