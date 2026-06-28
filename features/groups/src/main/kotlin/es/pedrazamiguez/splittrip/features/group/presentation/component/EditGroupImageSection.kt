package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.EditGroupUiState

@Composable
internal fun EditGroupImageSection(
    uiState: EditGroupUiState,
    onEvent: (EditGroupUiEvent) -> Unit
) {
    val hasImage = uiState.localGroupImagePath != null
    val isUploadEnabled = uiState.isCoverUploadEnabled

    if (isUploadEnabled) {
        GroupImagePreview(
            localGroupImagePath = uiState.localGroupImagePath,
            groupName = uiState.groupName
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        GroupImageActions(
            hasImage = hasImage,
            onSelectClick = { onEvent(EditGroupUiEvent.ShowImageSourceSheet(true)) },
            onRemoveClick = { onEvent(EditGroupUiEvent.GroupImageRemoved) }
        )

        GroupImageAttachmentHandler(
            showSheet = uiState.showImageSourceSheet,
            showRemoveOption = hasImage,
            onDismissSheet = { onEvent(EditGroupUiEvent.ShowImageSourceSheet(false)) },
            onImageSelected = { uri -> onEvent(EditGroupUiEvent.GroupImagePicked(uri)) },
            onImageRemoved = { onEvent(EditGroupUiEvent.GroupImageRemoved) }
        )
    } else {
        LockedGroupImagePreview()
    }
}
