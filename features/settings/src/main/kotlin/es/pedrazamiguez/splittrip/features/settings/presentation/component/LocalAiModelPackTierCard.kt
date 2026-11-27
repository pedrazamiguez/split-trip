package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocalAiModelPackTierCard(
    tier: LocalAiModelPackTierUiModel,
    onEvent: (LocalAiModelPackUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (tier.isInstalled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    FlatCard(
        modifier = modifier.fillMaxWidth(),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            Text(
                text = tier.displayName.asString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                PassportTag(text = tier.statusBadge.asString(), style = tier.statusTagStyle)
                if (tier.isRecommended && tier.statusTagStyle != PassportTagStyle.PRIMARY) {
                    PassportTag(
                        text = stringResource(R.string.local_ai_pack_badge_recommended),
                        style = PassportTagStyle.PRIMARY
                    )
                }
                PassportTag(text = tier.tierLabel.asString(), style = PassportTagStyle.NEUTRAL)
                PassportTag(text = tier.versionLabel.asString(), style = PassportTagStyle.NEUTRAL)
                PassportTag(text = tier.sizeLabel, style = PassportTagStyle.INFO)
            }

            Text(
                text = tier.requirementsLabel.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LocalAiModelPackTierFooter(tier = tier, onEvent = onEvent)
        }
    }
}
