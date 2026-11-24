package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText

private val PRO_CARD_ELEVATION = 4.dp

@Composable
internal fun PlanAllowancesTierCard(
    isProTier: Boolean,
    isGatingEnabled: Boolean,
    title: UiText,
    subtitle: UiText,
    badge: UiText,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isProTier) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val elevation = if (isProTier) PRO_CARD_ELEVATION else MaterialTheme.spacing.None
    val badgeStyle = when {
        !isGatingEnabled -> PassportTagStyle.SUCCESS
        isProTier -> PassportTagStyle.TERTIARY
        else -> PassportTagStyle.NEUTRAL
    }

    FlatCard(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SheetTitleText(
                    text = title.asString(),
                    modifier = Modifier.weight(1f)
                )
                PassportTag(
                    text = badge.asString(),
                    style = badgeStyle
                )
            }

            BodyText(
                text = subtitle.asString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
