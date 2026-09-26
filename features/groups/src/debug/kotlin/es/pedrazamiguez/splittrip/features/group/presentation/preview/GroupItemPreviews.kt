package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupItem

@PreviewComplete
@Composable
private fun GroupItemPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_2) {
        GroupItem(groupUiModel = it)
    }
}

@PreviewComplete
@Composable
private fun GroupItemNoImagePreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_NO_IMAGE) {
        GroupItem(groupUiModel = it)
    }
}

@PreviewComplete
@Composable
private fun GroupItemSingleExtraCurrencyPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_1) {
        GroupItem(groupUiModel = it)
    }
}

@PreviewComplete
@Composable
private fun GroupItemMultiCurrencyPreview() {
    GroupUiPreviewHelper(domainGroup = GROUP_DOMAIN_MULTI_CURRENCY) {
        GroupItem(groupUiModel = it)
    }
}
