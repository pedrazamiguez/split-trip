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
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

@Composable
fun GroupImageStep(
    uiState: CreateGroupUiState,
    onEvent: (CreateGroupUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasImage = uiState.localGroupImagePath != null

    WizardStepLayout(modifier = modifier) {
        GroupImageHeader()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        GroupImagePreview(
            localGroupImagePath = uiState.localGroupImagePath,
            groupName = uiState.groupName
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        GroupImageActions(
            hasImage = hasImage,
            onSelectClick = { onEvent(CreateGroupUiEvent.ShowImageSourceSheet(true)) },
            onRemoveClick = { onEvent(CreateGroupUiEvent.GroupImageRemoved) }
        )

        GroupImageAttachmentHandler(
            showSheet = uiState.showImageSourceSheet,
            showRemoveOption = hasImage,
            onDismissSheet = { onEvent(CreateGroupUiEvent.ShowImageSourceSheet(false)) },
            onImageSelected = { uri -> onEvent(CreateGroupUiEvent.GroupImagePicked(uri)) },
            onImageRemoved = { onEvent(CreateGroupUiEvent.GroupImageRemoved) }
        )
    }
}
