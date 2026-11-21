package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkType
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkUiModel

private val PERK_ICON_SIZE = 20.dp

@Composable
internal fun PlanAllowancePerkRow(
    perk: AllowancePerkUiModel,
    modifier: Modifier = Modifier
) {
    val leadingIcon = when (perk.type) {
        AllowancePerkType.MEMBERS_PER_GROUP -> TablerIcons.Outline.UsersPlus
        AllowancePerkType.SUBUNITS_PER_GROUP -> TablerIcons.Outline.Sitemap
        AllowancePerkType.AD_FREE -> TablerIcons.Outline.Shield
    }

    val iconTint = if (perk.isIncluded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    val textColor = if (perk.isIncluded) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val badgeIcon = if (perk.isIncluded) TablerIcons.Outline.Check else TablerIcons.Outline.Lock
    val badgeStyle = if (perk.isIncluded) PassportTagStyle.SECONDARY else PassportTagStyle.NEUTRAL

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(PERK_ICON_SIZE)
        )
        BodyText(
            text = perk.title.asString(),
            modifier = Modifier.weight(1f),
            color = textColor
        )
        PassportTag(
            text = perk.detail.asString(),
            icon = badgeIcon,
            style = badgeStyle
        )
    }
}
