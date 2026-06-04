package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.features.settings.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AiModelSelector(
    availableModels: ImmutableList<AiEngineType?>,
    selectedModel: AiEngineType?,
    resolvedModel: AiEngineType?,
    onModelSelected: (AiEngineType?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Text(
                text = stringResource(R.string.developer_services_ai_model_selection),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                availableModels.forEach { model ->
                    val label = when (model) {
                        null -> stringResource(R.string.developer_services_ai_model_automatic)
                        AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
                        AiEngineType.LITE_RT_LM -> stringResource(R.string.developer_services_ai_model_lite_rt)
                    }

                    PassportChip(
                        label = label,
                        selected = selectedModel == model,
                        onClick = { onModelSelected(model) }
                    )
                }
            }

            resolvedModel?.let { resolved ->
                val resolvedLabel = when (resolved) {
                    AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
                    AiEngineType.LITE_RT_LM -> stringResource(R.string.developer_services_ai_model_lite_rt)
                }
                Text(
                    text = stringResource(R.string.developer_services_ai_model_resolved_label, resolvedLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
