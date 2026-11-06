package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
        val (container, content) = when (style) {
            PassportTagStyle.PRIMARY,
            PassportTagStyle.SUCCESS ->
                MaterialTheme.colorScheme.primaryContainer to
                    MaterialTheme.colorScheme.onPrimaryContainer

            PassportTagStyle.SECONDARY ->
                MaterialTheme.colorScheme.secondaryContainer to
                    MaterialTheme.colorScheme.onSecondaryContainer

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
