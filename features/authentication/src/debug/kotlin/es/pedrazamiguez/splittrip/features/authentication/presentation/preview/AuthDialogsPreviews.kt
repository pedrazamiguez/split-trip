package es.pedrazamiguez.splittrip.features.authentication.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.CollisionMergeDialog
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.RegisterCollisionDialog
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

@PreviewComplete
@Composable
fun CollisionMergeDialogPreview() {
    PreviewThemeWrapper {
        CollisionMergeDialog(
            uiState = AuthenticationUiState(
                collisionEmail = "existing@example.com"
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
fun RegisterCollisionDialogPreview() {
    PreviewThemeWrapper {
        RegisterCollisionDialog(
            onEvent = {},
            onConfirmGoToLogin = {}
        )
    }
}
