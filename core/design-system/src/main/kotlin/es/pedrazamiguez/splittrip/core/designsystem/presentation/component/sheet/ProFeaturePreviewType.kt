package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit

enum class ProFeaturePreviewType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val highlightRes: Int,
    val icon: ImageVector
) {
    UNLIMITED_GROUPS(
        titleRes = R.string.pro_preview_unlimited_groups_title,
        descriptionRes = R.string.pro_preview_unlimited_groups_description,
        highlightRes = R.string.pro_preview_unlimited_groups_highlight,
        icon = TablerIcons.Outline.UsersGroup
    ),
    EXPANDED_GROUP_MEMBERS(
        titleRes = R.string.pro_preview_expanded_members_title,
        descriptionRes = R.string.pro_preview_expanded_members_description,
        highlightRes = R.string.pro_preview_expanded_members_highlight,
        icon = TablerIcons.Outline.UsersPlus
    ),
    GROUP_COVER_PHOTO(
        titleRes = R.string.pro_preview_group_cover_title,
        descriptionRes = R.string.pro_preview_group_cover_description,
        highlightRes = R.string.pro_preview_group_cover_highlight,
        icon = TablerIcons.Outline.Photo
    ),
    SUBUNIT_CREATION(
        titleRes = R.string.pro_preview_subunit_creation_title,
        descriptionRes = R.string.pro_preview_subunit_creation_description,
        highlightRes = R.string.pro_preview_subunit_creation_highlight,
        icon = TablerIcons.Outline.Sitemap
    ),
    AI_RECEIPT_SCANNING(
        titleRes = R.string.pro_preview_ai_receipt_title,
        descriptionRes = R.string.pro_preview_ai_receipt_description,
        highlightRes = R.string.pro_preview_ai_receipt_highlight,
        icon = TablerIcons.Outline.PhotoAi
    ),
    UNLIMITED_RECEIPTS(
        titleRes = R.string.pro_preview_unlimited_receipts_title,
        descriptionRes = R.string.pro_preview_unlimited_receipts_description,
        highlightRes = R.string.pro_preview_unlimited_receipts_highlight,
        icon = TablerIcons.Outline.Receipt
    );

    companion object {
        fun fromGatedLimit(limit: GatedLimit): ProFeaturePreviewType = when (limit) {
            GatedLimit.MAX_OWNED_GROUPS_COUNT -> UNLIMITED_GROUPS
            GatedLimit.MAX_MEMBERS_PER_GROUP -> EXPANDED_GROUP_MEMBERS
            GatedLimit.AI_RECEIPT_MONTHLY_SCANS -> AI_RECEIPT_SCANNING
            GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS -> UNLIMITED_RECEIPTS
        }

        fun fromGatedFeature(feature: GatedFeature): ProFeaturePreviewType = when (feature) {
            GatedFeature.GROUP_COVER_UPLOAD -> GROUP_COVER_PHOTO
            GatedFeature.SUBUNIT_CREATION -> SUBUNIT_CREATION
            GatedFeature.AI_RECEIPT_SCANNING -> AI_RECEIPT_SCANNING
            GatedFeature.AD_FREE -> UNLIMITED_GROUPS
        }
    }
}
