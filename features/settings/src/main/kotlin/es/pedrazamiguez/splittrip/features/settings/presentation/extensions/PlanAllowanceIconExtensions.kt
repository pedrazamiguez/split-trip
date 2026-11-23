package es.pedrazamiguez.splittrip.features.settings.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.AdOff
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaType

internal fun AllowanceQuotaType.toIconVector(): ImageVector = when (this) {
    AllowanceQuotaType.AI_RECEIPT_SCANS -> TablerIcons.Outline.PhotoAi
    AllowanceQuotaType.MEDIA_UPLOADS -> TablerIcons.Outline.Photo
    AllowanceQuotaType.REPORT_EXPORTS -> TablerIcons.Outline.Share
    AllowanceQuotaType.OWNED_GROUPS -> TablerIcons.Outline.UsersGroup
}

internal fun AllowancePerkType.toIconVector(): ImageVector = when (this) {
    AllowancePerkType.MEMBERS_PER_GROUP -> TablerIcons.Outline.UsersPlus
    AllowancePerkType.SUBUNITS_PER_GROUP -> TablerIcons.Outline.Sitemap
    AllowancePerkType.AD_FREE -> TablerIcons.Outline.AdOff
}
