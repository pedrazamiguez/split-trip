package es.pedrazamiguez.splittrip.core.designsystem.preview.update

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.AppUpdateDownloadingBanner
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun AppUpdateDownloadingBannerIndeterminatePreview() {
    PreviewThemeWrapper {
        AppUpdateDownloadingBanner(
            visible = true,
            progressFraction = null,
            progressPercent = null
        )
    }
}

@PreviewComplete
@Composable
private fun AppUpdateDownloadingBannerDeterminatePreview() {
    PreviewThemeWrapper {
        AppUpdateDownloadingBanner(
            visible = true,
            progressFraction = 0.64f,
            progressPercent = 64
        )
    }
}
