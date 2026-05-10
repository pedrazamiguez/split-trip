package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing

private val ACCENT_STRIP_WIDTH = 4.dp

/**
 * A reusable inline error banner for forms.
 *
 * Renders a [Surface] styled with the `errorContainer` color scheme token when [error] is
 * non-null, and renders nothing (emits no content) when [error] is null.
 *
 * In light mode a 4 dp left-side accent strip is drawn via [Modifier.drawBehind], adding a
 * visual anchor without an extra intrinsic measurement pass. The strip is omitted in dark mode
 * where `errorContainer` (`#93000A`) already provides strong contrast against the near-black
 * surface, preserving the dark-mode appearance unchanged.
 *
 * Typical usage is to surface server-side / submission failures directly inside the form layout,
 * as a complement to field-level validation errors.
 *
 * @param error The [UiText] to display, or null to hide the banner entirely.
 * @param modifier Optional modifier applied to the [Surface] container.
 */
@Composable
fun FormErrorBanner(
    error: UiText?,
    modifier: Modifier = Modifier
) {
    error?.let { errorUiText ->
        val isDarkTheme = isSystemInDarkTheme()
        val errorColor = MaterialTheme.colorScheme.error

        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = modifier.fillMaxWidth()
        ) {
            // drawBehind draws the accent strip at zero extra layout cost — no intrinsic pass.
            // The strip is light-mode only; dark mode's errorContainer is already high-contrast.
            val stripModifier = if (!isDarkTheme) {
                Modifier.drawBehind {
                    drawRect(
                        color = errorColor,
                        size = Size(width = ACCENT_STRIP_WIDTH.toPx(), height = size.height)
                    )
                }
            } else {
                Modifier
            }
            Box(modifier = stripModifier) {
                Text(
                    text = errorUiText.asString(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = if (!isDarkTheme) {
                            ACCENT_STRIP_WIDTH + MaterialTheme.spacing.Medium
                        } else {
                            MaterialTheme.spacing.Medium
                        },
                        top = MaterialTheme.spacing.Medium,
                        end = MaterialTheme.spacing.Medium,
                        bottom = MaterialTheme.spacing.Medium
                    )
                )
            }
        }
    }
}
