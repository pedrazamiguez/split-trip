package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent

@Composable
internal fun RegisterCollisionDialog(
    modifier: Modifier = Modifier,
    onEvent: (RegisterUiEvent) -> Unit,
    onConfirmGoToLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(RegisterUiEvent.DismissCollisionDialog) },
        title = {
            Text(
                text = stringResource(R.string.register_collision_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.register_collision_dialog_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.register_collision_dialog_confirm),
                onClick = onConfirmGoToLogin
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.register_collision_dialog_cancel),
                onClick = { onEvent(RegisterUiEvent.DismissCollisionDialog) }
            )
        },
        modifier = modifier
    )
}
