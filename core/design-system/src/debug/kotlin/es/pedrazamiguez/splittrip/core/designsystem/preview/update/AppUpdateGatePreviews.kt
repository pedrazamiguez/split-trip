package es.pedrazamiguez.splittrip.core.designsystem.preview.update

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.update.AppUpdateGate
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun AppUpdateGatePreview() {
    PreviewThemeWrapper {
        AppUpdateGate(
            onUpdateClick = {}
        )
    }
}

@PreviewComplete
@Composable
private fun AppUpdateGateUpdatingPreview() {
    PreviewThemeWrapper {
        AppUpdateGate(
            onUpdateClick = {},
            isUpdating = true
        )
    }
}
