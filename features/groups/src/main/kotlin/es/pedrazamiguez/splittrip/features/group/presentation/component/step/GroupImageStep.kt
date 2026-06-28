package es.pedrazamiguez.splittrip.features.group.presentation.component.step

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupImageActions
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupImageAttachmentHandler
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupImageHeader
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupImagePreview
import es.pedrazamiguez.splittrip.features.group.presentation.component.LockedGroupImagePreview
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

@Composable
fun GroupImageStep(
    uiState: CreateEditGroupUiState,
    onEvent: (CreateEditGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImage = uiState.localGroupImagePath != null
    val isUploadEnabled = uiState.isCoverUploadEnabled

    WizardStepLayout(modifier = modifier) {
        GroupImageHeader()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        if (isUploadEnabled) {
            GroupImagePreview(
                localGroupImagePath = uiState.localGroupImagePath,
                groupName = uiState.groupName
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

            GroupImageActions(
                hasImage = hasImage,
                onSelectClick = { onEvent(CreateEditGroupUiEvent.ShowImageSourceSheet(true)) },
                onRemoveClick = { onEvent(CreateEditGroupUiEvent.GroupImageRemoved) }
            )

            GroupImageAttachmentHandler(
                showSheet = uiState.showImageSourceSheet,
                showRemoveOption = hasImage,
                onDismissSheet = { onEvent(CreateEditGroupUiEvent.ShowImageSourceSheet(false)) },
                onImageSelected = { uri -> onEvent(CreateEditGroupUiEvent.GroupImagePicked(uri)) },
                onImageRemoved = { onEvent(CreateEditGroupUiEvent.GroupImageRemoved) }
            )
        } else {
            LockedGroupImagePreview()
        }
    }
}
