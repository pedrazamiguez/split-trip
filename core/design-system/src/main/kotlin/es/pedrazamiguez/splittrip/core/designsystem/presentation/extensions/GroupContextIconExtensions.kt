package es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Home
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PlaneTilt
import es.pedrazamiguez.splittrip.domain.enums.GroupContext

fun GroupContext.toIconVector(): ImageVector = when (this) {
    GroupContext.TRAVEL -> TablerIcons.Outline.PlaneTilt
    GroupContext.EVERYDAY -> TablerIcons.Outline.Home
}
