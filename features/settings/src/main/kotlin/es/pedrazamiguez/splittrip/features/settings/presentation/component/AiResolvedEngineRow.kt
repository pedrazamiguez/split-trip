package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.formatLiteRtDisplayLabel
import es.pedrazamiguez.splittrip.features.settings.presentation.extensions.toLabelRes
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiResolvedEngineRow(
    resolvedModel: AiEngineType,
    installedLocalAiPack: InstalledLocalAiPackInfo?,
    modifier: Modifier = Modifier
) {
    val localizedTierLabel = installedLocalAiPack?.tier?.let { stringResource(it.toLabelRes()) }
    val resolvedLabel = when (resolvedModel) {
        AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
        AiEngineType.LITE_RT_LM -> formatLiteRtDisplayLabel(
            baseLabel = stringResource(R.string.developer_services_ai_model_lite_rt),
            installedPack = installedLocalAiPack,
            localizedTierLabel = localizedTierLabel
        )
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
    ) {
        Text(
            text = stringResource(R.string.developer_services_ai_model_resolved_prefix),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        PassportTag(text = resolvedLabel, style = PassportTagStyle.INFO)
        if (resolvedModel == AiEngineType.LITE_RT_LM && localizedTierLabel != null) {
            PassportTag(text = localizedTierLabel, style = PassportTagStyle.NEUTRAL)
        } else if (resolvedModel == AiEngineType.LITE_RT_LM && installedLocalAiPack == null) {
            PassportTag(
                text = stringResource(R.string.local_ai_pack_badge_not_installed),
                style = PassportTagStyle.WARNING
            )
        }
    }
}
