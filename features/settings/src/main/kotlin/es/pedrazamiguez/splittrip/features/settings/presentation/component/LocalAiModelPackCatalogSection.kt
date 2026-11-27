package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.LocalAiModelPackTierUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun LocalAiModelPackCatalogSection(
    tiers: ImmutableList<LocalAiModelPackTierUiModel>,
    onEvent: (LocalAiModelPackUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SectionHeadingText(text = stringResource(R.string.local_ai_pack_section_available_models))
        tiers.forEach { tier ->
            key(tier.packId) {
                LocalAiModelPackTierCard(
                    tier = tier,
                    onEvent = onEvent
                )
            }
        }
    }
}
