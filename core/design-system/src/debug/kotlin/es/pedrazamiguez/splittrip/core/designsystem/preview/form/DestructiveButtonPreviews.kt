package es.pedrazamiguez.splittrip.core.designsystem.preview.form

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.DestructiveButton
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun DestructiveButtonEnabledPreview() {
    PreviewThemeWrapper {
        DestructiveButton(
            text = "Delete Group",
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun DestructiveButtonDisabledPreview() {
    PreviewThemeWrapper {
        DestructiveButton(
            text = "Delete Group",
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}

@PreviewThemes
@Composable
private fun DestructiveButtonLoadingPreview() {
    PreviewThemeWrapper {
        DestructiveButton(
            text = "Delete Group",
            onClick = {},
            isLoading = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
