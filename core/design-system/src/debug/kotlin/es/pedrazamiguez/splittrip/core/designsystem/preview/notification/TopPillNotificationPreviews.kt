package es.pedrazamiguez.splittrip.core.designsystem.preview.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillNotificationContent
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

private const val LARGE_FONT_SCALE = 1.5f

@PreviewThemes
@Composable
private fun TopPillNotificationSingleLinePreview() {
    PreviewThemeWrapper {
        TopPillNotificationContent(
            message = "Group updated successfully.",
            onDismiss = {}
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
            onDismiss = {}
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
            onDismiss = {}
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
                onDismiss = {}
            )
        }
    }
}
