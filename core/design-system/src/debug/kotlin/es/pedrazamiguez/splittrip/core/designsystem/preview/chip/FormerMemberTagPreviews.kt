package es.pedrazamiguez.splittrip.core.designsystem.preview.chip

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.FormerMemberTag
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun FormerMemberTagPreview() {
    PreviewThemeWrapper {
        FormerMemberTag(modifier = Modifier.padding(16.dp))
    }
}
