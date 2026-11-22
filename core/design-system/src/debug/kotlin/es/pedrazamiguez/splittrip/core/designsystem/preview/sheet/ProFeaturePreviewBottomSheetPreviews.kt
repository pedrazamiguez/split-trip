package es.pedrazamiguez.splittrip.core.designsystem.preview.sheet

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewComplete
@Composable
private fun ProFeaturePreviewUnlimitedGroupsPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.UNLIMITED_GROUPS,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}

@PreviewThemes
@Composable
private fun ProFeaturePreviewExpandedMembersPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}

@PreviewThemes
@Composable
private fun ProFeaturePreviewGroupCoverPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.GROUP_COVER_PHOTO,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}

@PreviewThemes
@Composable
private fun ProFeaturePreviewSubunitCreationPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.SUBUNIT_CREATION,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ProFeaturePreviewAiReceiptScanningPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.AI_RECEIPT_SCANNING,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ProFeaturePreviewUnlimitedReceiptsPreview() {
    PreviewThemeWrapper {
        ProFeaturePreviewBottomSheet(
            previewType = ProFeaturePreviewType.UNLIMITED_RECEIPTS,
            onUpgrade = {},
            onDismiss = {}
        )
    }
}
