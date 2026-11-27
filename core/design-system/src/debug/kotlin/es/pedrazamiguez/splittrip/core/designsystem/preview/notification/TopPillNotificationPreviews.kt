package es.pedrazamiguez.splittrip.core.designsystem.preview.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillNotificationContent
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

private const val LARGE_FONT_SCALE = 1.5f

@PreviewThemes
@Composable
private fun TopPillNotificationAllStylesPreview() {
    PreviewThemeWrapper {
        Column(
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            TopPillStyle.entries.forEach { style ->
                TopPillNotificationContent(
                    message = when (style) {
                        TopPillStyle.NEUTRAL -> "Neutral status notification."
                        TopPillStyle.INFO -> "Info notification with subtle sky blue tint."
                        TopPillStyle.SUCCESS -> "Group updated successfully."
                        TopPillStyle.WARNING -> "Approaching free plan group limit."
                        TopPillStyle.ERROR -> "Could not load subscription options."
                    },
                    onDismiss = {},
                    style = style
                )
            }
        }
    }
}

@PreviewThemes
@Composable
private fun TopPillNotificationSingleLinePreview() {
    PreviewThemeWrapper {
        TopPillNotificationContent(
            message = "Group updated successfully.",
            onDismiss = {},
            style = TopPillStyle.SUCCESS
        )
    }
}

@PreviewThemes
@Composable
private fun TopPillNotificationMultiLinePreview() {
    PreviewThemeWrapper {
        TopPillNotificationContent(
            message = "Las cuentas gratuitas permiten 1 grupo. " +
                "Mejora a SplitTrip Pro para crear más grupos y añadir más miembros.",
            onDismiss = {},
            style = TopPillStyle.WARNING
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun TopPillNotificationNarrowMultiLinePreview() {
    PreviewThemeWrapper {
        TopPillNotificationContent(
            message = "Las cuentas gratuitas permiten 1 grupo. " +
                "Mejora a SplitTrip Pro para crear más grupos y añadir más miembros.",
            onDismiss = {},
            style = TopPillStyle.ERROR
        )
    }
}

@PreviewThemes
@Composable
private fun TopPillNotificationLargeFontPreview() {
    PreviewThemeWrapper {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density = currentDensity.density, fontScale = LARGE_FONT_SCALE)
        ) {
            TopPillNotificationContent(
                message = "Las cuentas gratuitas permiten 1 grupo. " +
                    "Mejora a SplitTrip Pro para crear más grupos y añadir más miembros.",
                onDismiss = {},
                style = TopPillStyle.WARNING
            )
        }
    }
}
