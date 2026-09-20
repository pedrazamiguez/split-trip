package es.pedrazamiguez.splittrip.features.profile.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.profile.presentation.component.AvatarEditor
import es.pedrazamiguez.splittrip.features.profile.presentation.component.AvatarSelectionSheet

@PreviewComplete
@Composable
fun AvatarEditorPreview() {
    PreviewThemeWrapper {
        AvatarEditor(
            avatarUrl = null,
            localAvatarPath = null,
            onClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun AvatarSelectionSheetPreview() {
    PreviewThemeWrapper {
        AvatarSelectionSheet(
            showRemoveOption = true,
            onCameraSelected = {},
            onGallerySelected = {},
            onRemoveSelected = {},
            onDismiss = {}
        )
    }
}
