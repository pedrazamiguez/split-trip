package es.pedrazamiguez.splittrip.core.designsystem.preview.update

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.AppUpdateReadyBanner
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun AppUpdateReadyBannerPreview() {
    PreviewThemeWrapper {
        AppUpdateReadyBanner(
            visible = true,
            onRestartClick = {}
        )
    }
}
