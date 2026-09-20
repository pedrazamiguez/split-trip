package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCard

/** Hero card with a cover image and multiple member avatars. */
@PreviewComplete
@Composable
private fun SelectedGroupCardWithImagePreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) {
        SelectedGroupCard(
            groupUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Hero card without a cover image — shows the gradient placeholder. */
@PreviewComplete
@Composable
private fun SelectedGroupCardNoImagePreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_NO_IMAGE) {
        SelectedGroupCard(
            groupUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Hero card in all themes and locales. */
@PreviewComplete
@Composable
private fun SelectedGroupCardThemesPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_NO_IMAGE) {
        SelectedGroupCard(
            groupUiModel = it,
            modifier = Modifier.padding(16.dp)
        )
    }
}
