package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString

/**
 * A reusable inline error banner for forms.
 *
 * Renders a [Surface] styled with the `errorContainer` color scheme token when [error] is
 * non-null, and renders nothing (emits no content) when [error] is null.
 *
 * A 4 dp left-side accent strip using the full `error` token acts as a visual anchor in light
 * mode, where `errorContainer` alone provides low salience against the off-white page background.
 * The strip is clipped to the Surface shape and renders in both themes — in dark mode it
 * complements the already high-contrast `errorContainer` fill without changing perceived weight.
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
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = modifier.fillMaxWidth()
        ) {
            // IntrinsicSize.Min lets the accent strip fillMaxHeight relative to the text row
            // without creating a circular height measurement dependency.
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                )
                Text(
                    text = errorUiText.asString(),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
