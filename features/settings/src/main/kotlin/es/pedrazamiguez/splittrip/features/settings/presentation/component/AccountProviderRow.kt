package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.features.settings.R

private val PROVIDER_BADGE_SIZE = 40.dp
private val PROVIDER_ICON_SIZE = 20.dp

@Composable
internal fun AccountProviderRow(
    name: String,
    icon: ImageVector,
    isLinked: Boolean,
    onLinkClick: () -> Unit,
    onUnlinkClick: () -> Unit,
    canUnlink: Boolean,
    isActionLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val badgeBg = if (isLinked) colors.primaryContainer else colors.surfaceContainerHigh
    val badgeTint = if (isLinked) colors.onPrimaryContainer else colors.onSurfaceVariant
    val statusRes = if (isLinked) R.string.account_status_linked else R.string.account_status_not_linked
    val statusIcon = if (isLinked) TablerIcons.Outline.Check else null
    val statusStyle = if (isLinked) PassportTagStyle.SUCCESS else PassportTagStyle.NEUTRAL

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PROVIDER_BADGE_SIZE)
                .clip(MaterialTheme.shapes.medium)
                .background(badgeBg)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeTint,
                modifier = Modifier.size(PROVIDER_ICON_SIZE)
            )
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            CardTitleText(text = name)
            PassportTag(text = stringResource(statusRes), icon = statusIcon, style = statusStyle)
        }

        Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))

        AccountProviderActionButton(
            isLinked = isLinked,
            isActionLoading = isActionLoading,
            canUnlink = canUnlink,
            onLinkClick = onLinkClick,
            onUnlinkClick = onUnlinkClick
        )
    }
}
